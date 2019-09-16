package io.onedev.server.plugin.docker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.SystemUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import io.onedev.commons.launcher.bootstrap.Bootstrap;
import io.onedev.commons.launcher.loader.AppLoader;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.Maps;
import io.onedev.commons.utils.PathUtils;
import io.onedev.commons.utils.StringUtils;
import io.onedev.commons.utils.command.Commandline;
import io.onedev.commons.utils.command.ExecuteResult;
import io.onedev.commons.utils.command.LineConsumer;
import io.onedev.commons.utils.command.ProcessKiller;
import io.onedev.commons.utils.concurrent.CapacityRunner;
import io.onedev.k8shelper.CacheInstance;
import io.onedev.k8shelper.KubernetesHelper;
import io.onedev.server.OneDev;
import io.onedev.server.OneException;
import io.onedev.server.ci.job.JobContext;
import io.onedev.server.ci.job.JobManager;
import io.onedev.server.ci.job.JobService;
import io.onedev.server.ci.job.SubmoduleCredential;
import io.onedev.server.ci.job.Variable;
import io.onedev.server.git.config.GitConfig;
import io.onedev.server.model.support.RegistryLogin;
import io.onedev.server.model.support.jobexecutor.JobExecutor;
import io.onedev.server.plugin.docker.DockerExecutor.TestData;
import io.onedev.server.util.JobLogger;
import io.onedev.server.util.PKCS12CertExtractor;
import io.onedev.server.util.ServerConfig;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.util.validation.annotation.DnsName;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Horizontal;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.util.Testable;

@Editable(order=200, description="This executor runs CI jobs as docker containers on OneDev server")
@ClassValidating
@Horizontal
public class DockerExecutor extends JobExecutor implements Testable<TestData>, Validatable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(DockerExecutor.class);

	private String networkPrefix = "onedev-ci";
	
	private List<RegistryLogin> registryLogins = new ArrayList<>();
	
	private int capacity = Runtime.getRuntime().availableProcessors();
	
	private String runOptions;
	
	private String dockerExecutable;
	
	private transient CapacityRunner capacityRunner;

	@Editable(order=30, description="OneDev will create a separate docker network to run each "
			+ "job for isolation purpose. Here you may specify prefix of the network to identify "
			+ "job containers created by this executor")
	@DnsName
	@NotEmpty
	public String getNetworkPrefix() {
		return networkPrefix;
	}

	public void setNetworkPrefix(String networkPrefix) {
		this.networkPrefix = networkPrefix;
	}

	@Editable(order=400, description="Specify login information for docker registries if necessary")
	public List<RegistryLogin> getRegistryLogins() {
		return registryLogins;
	}

	public void setRegistryLogins(List<RegistryLogin> registryLogins) {
		this.registryLogins = registryLogins;
	}

	@Editable(order=475, description="Specify max number of concurrent jobs being executed. Each job execution "
			+ "will launch a separate docker container. Defaults to number of processors in the system")
	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Editable(order=50050, group="More Settings", description="Optionally specify options to run container. For instance, you may use <tt>-m 2g</tt> "
			+ "to limit memory of created container to be 2 giga bytes")
	public String getRunOptions() {
		return runOptions;
	}

	public void setRunOptions(String runOptions) {
		this.runOptions = runOptions;
	}

	@Editable(order=50100, group="More Settings", description="Optionally specify docker executable, for instance <i>/usr/local/bin/docker</i>. "
			+ "Leave empty to use docker executable in PATH")
	@NameOfEmptyValue("Use default")
	public String getDockerExecutable() {
		return dockerExecutable;
	}

	public void setDockerExecutable(String dockerExecutable) {
		this.dockerExecutable = dockerExecutable;
	}

	private Commandline newDocker() {
		if (getDockerExecutable() != null)
			return new Commandline(getDockerExecutable());
		else
			return new Commandline("docker");
	}
	
	private String getImageOS(JobLogger jobLogger, String image) {
		Commandline docker = newDocker();
		docker.addArgs("image", "inspect", "-f", "{{.Os}}", image);
		
		AtomicReference<String> osRef = new AtomicReference<>(null);
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				osRef.set(line);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
		
		return Preconditions.checkNotNull(osRef.get());
	}
	
	private synchronized CapacityRunner getCapacityRunner() {
		if (capacityRunner == null)
			capacityRunner = new CapacityRunner(capacity);
		return capacityRunner;
	}
	
	private File getCacheHome() {
		return new File(Bootstrap.getSiteDir(), "job-cache"); 
	}

	private String createNetwork(JobContext jobContext, JobLogger jobLogger) {
		String network = getNetworkPrefix() + "-" + jobContext.getProjectName() + "-" + jobContext.getBuildNumber();
		
		AtomicBoolean networkExists = new AtomicBoolean(false);
		Commandline docker = newDocker();
		docker.addArgs("network", "ls", "-q", "--filter", "name=" + network);
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				networkExists.set(true);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
		
		if (networkExists.get()) {
			clearNetwork(network, jobLogger);
		} else {
			docker.clearArgs();
			docker.addArgs("network", "create", network);
			docker.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.debug(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();
		}
		
		return network;
	}
	
	private void deleteNetwork(String network, JobLogger jobLogger) {
		clearNetwork(network, jobLogger);
		
		Commandline docker = newDocker();
		docker.clearArgs();
		docker.addArgs("network", "rm", network);
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				logger.debug(line);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
	}
	
	private void clearNetwork(String network, JobLogger jobLogger) {
		Commandline docker = newDocker();
		
		List<String> containerIds = new ArrayList<>();
		docker.addArgs("ps", "-a", "-q", "--filter", "network=" + network);
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				containerIds.add(line);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
		
		for (String container: containerIds) {
			docker.clearArgs();
			docker.addArgs("container", "stop", container);
			docker.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.debug(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();
			
			docker.clearArgs();
			docker.addArgs("container", "rm", container);
			docker.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.debug(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();
		}
	}
	
	@SuppressWarnings("resource")
	private void startService(String network, JobService jobService, JobLogger jobLogger) {
		jobLogger.log("Pulling service image...") ;
		Commandline docker = newDocker();
		docker.addArgs("pull", jobService.getImage());
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				logger.debug(line);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
		
		jobLogger.log("Creating service container...");
		
		String containerName = network + "-service-" + jobService.getName();
		docker.clearArgs();
		docker.addArgs("run", "-d", "--name=" + containerName, "--network=" + network, 
				"--network-alias=" + jobService.getName());
		for (Variable var: jobService.getEnvVars()) 
			docker.addArgs("--env", var.getName() + "=" + var.getValue());
		docker.addArgs(jobService.getImage());
		if (jobService.getArguments() != null) {
			for (String token: StringUtils.parseQuoteTokens(jobService.getArguments()))
				docker.addArgs(token);
		}
		
		docker.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();

		jobLogger.log("Waiting for service to be ready...");
		
		boolean isWindows = getImageOS(jobLogger, jobService.getImage()).equalsIgnoreCase("windows");
		ObjectMapper jsonReader = OneDev.getInstance(ObjectMapper.class);		
		while (true) {
			StringBuilder builder = new StringBuilder();
			docker.clearArgs();
			docker.addArgs("inspect", containerName);
			docker.execute(new LineConsumer(Charsets.UTF_8.name()) {

				@Override
				public void consume(String line) {
					builder.append(line).append("\n");
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();

			JsonNode stateNode;
			try {
				stateNode = jsonReader.readTree(builder.toString()).iterator().next().get("State");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			if (stateNode.get("Status").asText().equals("running")) {
				docker.clearArgs();
				docker.addArgs("exec", containerName);
				if (isWindows) 
					docker.addArgs("cmd", "/c", jobService.getReadinessCheckCommand());
				else 
					docker.addArgs("sh", "-c", jobService.getReadinessCheckCommand());
				
				ExecuteResult result = docker.execute(new LineConsumer() {

					@Override
					public void consume(String line) {
						jobLogger.log("Service readiness check: " + line);
					}
					
				}, new LineConsumer() {

					@Override
					public void consume(String line) {
						jobLogger.log("Service readiness check: " + line);
					}
					
				});
				if (result.getReturnCode() == 0) {
					jobLogger.log("Service is ready");
					break;
				}
			} else if (stateNode.get("Status").asText().equals("exited")) {
				if (stateNode.get("OOMKilled").asText().equals("true"))  
					jobLogger.log("Out of memory");
				else if (stateNode.get("Error").asText().length() != 0)  
					jobLogger.log(stateNode.get("Error").asText());
				
				docker.clearArgs();
				docker.addArgs("logs", containerName);
				docker.execute(new LineConsumer(Charsets.UTF_8.name()) {

					@Override
					public void consume(String line) {
						jobLogger.log(line);
					}
					
				}, new LineConsumer(Charsets.UTF_8.name()) {

					@Override
					public void consume(String line) {
						jobLogger.log(line);
					}
					
				}).checkReturnCode();
				
				throw new OneException(String.format("Service '" + jobService.getName() + "' is stopped unexpectedly"));
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}		
	}
	
	@Override
	public void execute(String jobToken, JobContext jobContext) {
		File hostCIHome = FileUtils.createTempDir("onedev-ci");
		try {
			JobLogger jobLogger = jobContext.getLogger();
			
			getCapacityRunner().call(new Callable<Void>() {
	
				@SuppressWarnings("resource")
				@Override
				public Void call() {
					String network = createNetwork(jobContext, jobLogger);
					jobLogger.log("Executing job with docker under network " + network + "...");
					try {
						jobContext.notifyJobRunning();
						
						JobManager jobManager = OneDev.getInstance(JobManager.class);		
						File hostCacheHome = getCacheHome();
						FileUtils.createDir(hostCacheHome);
						
						jobLogger.log("Allocating job caches...") ;
						Map<CacheInstance, Date> cacheInstances = KubernetesHelper.getCacheInstances(hostCacheHome);
						Map<CacheInstance, String> cacheAllocations = jobManager.allocateJobCaches(jobToken, new Date(), cacheInstances);
						KubernetesHelper.preprocess(hostCacheHome, cacheAllocations, new Consumer<File>() {
		
							@Override
							public void accept(File directory) {
								cleanDirAsRoot(directory);
							}
							
						});
							
						login(jobLogger);

						for (JobService jobService: jobContext.getServices()) {
							jobLogger.log("Starting service '" + jobService.getName() + "...");
							startService(network, jobService, jobLogger);
						}
						
						jobLogger.log("Pulling job image...") ;
						Commandline docker = newDocker();
						docker.addArgs("pull", jobContext.getImage());
						docker.execute(new LineConsumer() {

							@Override
							public void consume(String line) {
								logger.debug(line);
							}
							
						}, new LineConsumer() {

							@Override
							public void consume(String line) {
								jobLogger.log(line);
							}
							
						}).checkReturnCode();
						
						boolean isWindows = getImageOS(jobLogger, jobContext.getImage()).equalsIgnoreCase("windows");
						
						File workspaceCache = null;
						for (Map.Entry<CacheInstance, String> entry: cacheAllocations.entrySet()) {
							if (PathUtils.isCurrent(entry.getValue())) {
								workspaceCache = entry.getKey().getDirectory(hostCacheHome);
								break;
							}
						}
						
						File hostWorkspace;
						if (workspaceCache != null) {
							hostWorkspace = workspaceCache;
						} else { 
							hostWorkspace = new File(hostCIHome, "workspace");
							FileUtils.createDir(hostWorkspace);
						}
						
						if (jobContext.isRetrieveSource()) {
							jobLogger.log("Retrieving source code...");
							File tempHome = FileUtils.createTempDir();
							try {
								Map<String, String> environments = Maps.newHashMap("HOME", tempHome.getAbsolutePath());
								Commandline git = new Commandline(AppLoader.getInstance(GitConfig.class).getExecutable());	
								git.environments(environments).workingDir(hostWorkspace);
								LineConsumer logger = new LineConsumer() {
	
									@Override
									public void consume(String line) {
										jobContext.getLogger().log(line);
									}
									
								};
								
								git.addArgs("config", "--global", "credential.modalprompt", "false");
								git.execute(logger, logger).checkReturnCode();
								
								// clear credential.helper list to remove possible Windows credential manager
								git.clearArgs();
								if (SystemUtils.IS_OS_WINDOWS)
									git.addArgs("config", "--global", "credential.helper", "\"\"");
								else
									git.addArgs("config", "--global", "credential.helper", "");
								git.execute(logger, logger).checkReturnCode();
								
								git.clearArgs();
								git.addArgs("config", "--global", "--add", "credential.helper", "store");
								git.execute(logger, logger).checkReturnCode();
								
								git.clearArgs();
								git.addArgs("config", "--global", "credential.useHttpPath", "true");
								git.execute(logger, logger).checkReturnCode();
	
								List<String> trustCertContent = new ArrayList<>();
								ServerConfig serverConfig = OneDev.getInstance(ServerConfig.class); 
								File keystoreFile = serverConfig.getKeystoreFile();
								if (keystoreFile != null) {
									String password = serverConfig.getKeystorePassword();
									for (Map.Entry<String, String> entry: new PKCS12CertExtractor(keystoreFile, password).extact().entrySet()) 
										trustCertContent.addAll(Splitter.on('\n').trimResults().splitToList(entry.getValue()));
								}
								if (serverConfig.getTrustCertsDir() != null) {
									for (File file: serverConfig.getTrustCertsDir().listFiles()) {
										if (file.isFile()) 
											trustCertContent.addAll(FileUtils.readLines(file));
									}
								}
	
								if (!trustCertContent.isEmpty()) {
									File trustCertFile = new File(tempHome, "trust-cert.pem");
									FileUtils.writeLines(trustCertFile, trustCertContent, "\n");
									git.clearArgs();
									git.addArgs("config", "--global", "http.sslCAInfo", trustCertFile.getAbsolutePath());
									git.execute(logger, logger).checkReturnCode();
								}
								
								if (!jobContext.getSubmoduleCredentials().isEmpty()) {
									List<String> submoduleCredentials = new ArrayList<>();
									for (SubmoduleCredential submoduleCredential: jobContext.getSubmoduleCredentials()) {
										String url = submoduleCredential.getUrl();
										String userName = URLEncoder.encode(submoduleCredential.getUserName(), Charsets.UTF_8.name());
										String password = URLEncoder.encode(submoduleCredential.getPasswordSecret(), Charsets.UTF_8.name());
										if (url.startsWith("http://")) {
											submoduleCredentials.add("http://" + userName + ":" + password 
													+ "@" + url.substring("http://".length()).replace(":", "%3a"));
										} else {
											submoduleCredentials.add("https://" + userName + ":" + password 
													+ "@" + url.substring("https://".length()).replace(":", "%3a"));
										}
									}
									FileUtils.writeLines(new File(tempHome, ".git-credentials"), submoduleCredentials, "\n");
								}
								
								if (!new File(hostWorkspace, ".git").exists()) {
									git.clearArgs();
									git.addArgs("init", ".");
									git.execute(logger, logger).checkReturnCode();
								}								
								
								git.clearArgs();
								git.addArgs("fetch", jobContext.getProjectGitDir().getAbsolutePath(), "--force", "--quiet", 
										"--depth=1", jobContext.getCommitId().name());
								git.execute(logger, logger).checkReturnCode();
								
								git.clearArgs();
								git.addArgs("checkout", "--quiet", jobContext.getCommitId().name());
								git.execute(logger, logger).checkReturnCode();
								
								// deinit submodules in case submodule url is changed
								git.clearArgs();
								git.addArgs("submodule", "deinit", "--all", "--force", "--quiet");
								git.execute(logger, new LineConsumer() {
	
									@Override
									public void consume(String line) {
										if (!line.contains("error: could not lock config file") && 
												!line.contains("warning: Could not unset core.worktree setting in submodule")) {
											jobContext.getLogger().log(line);
										}
									}
									
								}).checkReturnCode();
								
								git.clearArgs();
								git.addArgs("submodule", "update", "--init", "--recursive", "--force", "--quiet", "--depth=1");
								git.execute(logger, logger).checkReturnCode();
							} catch (IOException e) {
								throw new RuntimeException(e);
							} finally {
								FileUtils.deleteDir(tempHome);
							}
						}
						
						jobLogger.log("Copying job dependencies...");
						try {
							FileUtils.copyDirectory(jobContext.getServerWorkspace(), hostWorkspace);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
	
						String containerCIHome;
						String containerWorkspace;
						String[] containerCommand;
						if (isWindows) {
							containerCIHome = "C:\\onedev-ci";
							containerWorkspace = "C:\\onedev-ci\\workspace";
							containerCommand = new String[] {"cmd", "/c", "C:\\onedev-ci\\job-commands.bat"};						
	
							File scriptFile = new File(hostCIHome, "job-commands.bat");
							try {
								FileUtils.writeLines(scriptFile, jobContext.getCommands(), "\r\n");
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						} else {
							containerCIHome = "/onedev-ci";
							containerWorkspace = "/onedev-ci/workspace";
							containerCommand = new String[] {"sh", "/onedev-ci/job-commands.sh"};
							
							File scriptFile = new File(hostCIHome, "job-commands.sh");
							try {
								FileUtils.writeLines(scriptFile, jobContext.getCommands(), "\n");
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
						
						String containerName = network + "-job";
						docker.clearArgs();
						docker.addArgs("run", "--name=" + containerName, "--network=" + network);
						for (Map.Entry<String, String> entry: jobContext.getEnvVars().entrySet())
							docker.addArgs("--env", entry.getKey() + "=" + entry.getValue());
						if (getRunOptions() != null)
							docker.addArgs(StringUtils.parseQuoteTokens(getRunOptions()));
						
						docker.addArgs("-v", hostCIHome.getAbsolutePath() + ":" + containerCIHome);
						if (workspaceCache != null)
							docker.addArgs("-v", workspaceCache.getAbsolutePath() + ":" + containerWorkspace);
						for (Map.Entry<CacheInstance, String> entry: cacheAllocations.entrySet()) {
							if (!PathUtils.isCurrent(entry.getValue())) {
								String hostCachePath = entry.getKey().getDirectory(hostCacheHome).getAbsolutePath();
								String containerCachePath = PathUtils.resolve(containerWorkspace, entry.getValue());
								docker.addArgs("-v", hostCachePath + ":" + containerCachePath);
							}
						}
						if (SystemUtils.IS_OS_LINUX)
							docker.addArgs("-v", "/var/run/docker.sock:/var/run/docker.sock");
						
						docker.addArgs("-w", containerWorkspace, jobContext.getImage());
						docker.addArgs(containerCommand);
						
						jobLogger.log("Running job container...");
						
						try {
							docker.execute(new LineConsumer(Charsets.UTF_8.name()) {

								@Override
								public void consume(String line) {
									jobLogger.log(line);
								}
								
							}, new LineConsumer(Charsets.UTF_8.name()) {

								@Override
								public void consume(String line) {
									jobLogger.log(line);
								}
								
							}, null, new ProcessKiller() {
		
								@Override
								public void kill(Process process, String executionId) {
									jobLogger.log("Stopping job container...");
									Commandline cmd = newDocker();
									cmd.addArgs("stop", containerName);
									cmd.execute(new LineConsumer() {

										@Override
										public void consume(String line) {
											logger.debug(line);
										}
										
									}, new LineConsumer() {

										@Override
										public void consume(String line) {
											jobLogger.log(line);
										}
										
									}).checkReturnCode();
								}
								
							}).checkReturnCode();
						} finally {
							jobLogger.log("Sending job outcomes...");
							
							int baseLen = hostWorkspace.getAbsolutePath().length()+1;
							for (File file: jobContext.getCollectFiles().listFiles(hostWorkspace)) {
								try {
									FileUtils.copyFile(file, new File(jobContext.getServerWorkspace(), file.getAbsolutePath().substring(baseLen)));
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							}
						}
						jobLogger.log("Reporting job caches...");
						
						jobManager.reportJobCaches(jobToken, KubernetesHelper.getCacheInstances(hostCacheHome).keySet());
						
						return null;
					} finally {
						deleteNetwork(network, jobLogger);
					}
				}
				
			});
		} finally {
			cleanDirAsRoot(hostCIHome);
			FileUtils.deleteDir(hostCIHome);
		}
	}

	private void login(JobLogger jobLogger) {
		for (RegistryLogin login: getRegistryLogins()) {
			if (login.getRegistryUrl() != null)
				jobLogger.log(String.format("Login to docker registry '%s'...", login.getRegistryUrl()));
			else
				jobLogger.log("Login to official docker registry...");
			Commandline cmd = newDocker();
			cmd.addArgs("login", "-u", login.getUserName(), "--password-stdin");
			if (login.getRegistryUrl() != null)
				cmd.addArgs(login.getRegistryUrl());
			ByteArrayInputStream input;
			try {
				input = new ByteArrayInputStream(login.getPassword().getBytes(Charsets.UTF_8.name()));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			cmd.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.debug(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}, input).checkReturnCode();
		}
	}
	
	private boolean hasOptions(String[] arguments, String... options) {
		for (String argument: arguments) {
			for (String option: options) {
				if (option.startsWith("--")) {
					if (argument.startsWith(option + "=") || argument.equals(option))
						return true;
				} else if (option.startsWith("-")) {
					if (argument.startsWith(option))
						return true;
				} else {
					throw new RuntimeException("Invalid option: " + option);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		Set<String> registryUrls = new HashSet<>();
		for (RegistryLogin login: getRegistryLogins()) {
			if (!registryUrls.add(login.getRegistryUrl())) {
				isValid = false;
				String message;
				if (login.getRegistryUrl() != null)
					message = "Duplicate login entry for registry '" + login.getRegistryUrl() + "'";
				else
					message = "Duplicate login entry for official registry";
				context.buildConstraintViolationWithTemplate(message)
						.addPropertyNode("registryLogins").addConstraintViolation();
				break;
			}
		}
		if (getRunOptions() != null) {
			String[] arguments = StringUtils.parseQuoteTokens(getRunOptions());
			String invalidOptions[] = new String[] {"-w", "--workdir", "-d", "--detach", "-a", "--attach", "-t", "--tty", 
					"-i", "--interactive", "--rm", "--restart", "--name"}; 
			if (hasOptions(arguments, invalidOptions)) {
				StringBuilder errorMessage = new StringBuilder("Can not use options: "
						+ Joiner.on(", ").join(invalidOptions));
				context.buildConstraintViolationWithTemplate(errorMessage.toString())
						.addPropertyNode("runOptions").addConstraintViolation();
				isValid = false;
			} 
		}
		if (!isValid)
			context.disableDefaultConstraintViolation();
		return isValid;
	}
	
	@Override
	public void test(TestData testData, JobLogger jobLogger) {
		login(jobLogger);
		
		jobLogger.log("Pulling image...");
		
		Commandline cmd = newDocker();
		cmd.addArgs("pull", testData.getDockerImage());
		cmd.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				logger.debug(line);
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				jobLogger.log(line);
			}
			
		}).checkReturnCode();
		
		boolean windows = getImageOS(jobLogger, testData.getDockerImage()).equals("windows");
		
		jobLogger.log("Running container...");
		File workspaceDir = null;
		File cacheDir = null;
		try {
			workspaceDir = Bootstrap.createTempDir("workspace");
			cacheDir = new File(getCacheHome(), UUID.randomUUID().toString());
			FileUtils.createDir(cacheDir);
			
			cmd.clearArgs();
			cmd.addArgs("run", "--rm");
			if (getRunOptions() != null)
				cmd.addArgs(StringUtils.parseQuoteTokens(getRunOptions()));
			String containerWorkspacePath;
			String containerCachePath;
			if (windows) {
				containerWorkspacePath = "C:\\onedev-ci\\workspace";
				containerCachePath = "C:\\onedev-ci\\cache";
			} else {
				containerWorkspacePath = "/onedev-ci/workspace";
				containerCachePath = "/onedev-ci/cache";
			}
			cmd.addArgs("-v", workspaceDir.getAbsolutePath() + ":" + containerWorkspacePath);
			cmd.addArgs("-v", cacheDir.getAbsolutePath() + ":" + containerCachePath);
			
			cmd.addArgs("-w", containerWorkspacePath);
			cmd.addArgs(testData.getDockerImage());
			
			if (windows) 
				cmd.addArgs("cmd", "/c", "echo hello from container");
			else 
				cmd.addArgs("sh", "-c", "echo hello from container");
			
			cmd.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();
		} finally {
			if (workspaceDir != null)
				FileUtils.deleteDir(workspaceDir);
			if (cacheDir != null)
				FileUtils.deleteDir(cacheDir);
		}
		
		if (!SystemUtils.IS_OS_WINDOWS) {
			jobLogger.log("Checking busybox...");
			cmd = newDocker();
			cmd.addArgs("run", "--rm", "busybox", "sh", "-c", "echo hello from busybox");			
			cmd.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					jobLogger.log(line);
				}
				
			}).checkReturnCode();
		}
	}
	
	public void cleanDirAsRoot(File dir) {
		if (SystemUtils.IS_OS_WINDOWS) {
			FileUtils.cleanDir(dir);
		} else {
			Commandline cmd = newDocker();
			String containerPath = "/dir-to-clean";
			cmd.addArgs("run", "-v", dir.getAbsolutePath() + ":" + containerPath, "--rm", 
					"busybox", "sh", "-c", "rm -rf " + containerPath + "/*");			
			cmd.execute(new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.info(line);
				}
				
			}, new LineConsumer() {

				@Override
				public void consume(String line) {
					logger.error(line);
				}
				
			}).checkReturnCode();
		}
	}
	
	@Editable(name="Specify a Docker Image to Test Against")
	public static class TestData implements Serializable {

		private static final long serialVersionUID = 1L;

		private String dockerImage;

		@Editable
		@OmitName
		@NotEmpty
		public String getDockerImage() {
			return dockerImage;
		}

		public void setDockerImage(String dockerImage) {
			this.dockerImage = dockerImage;
		}
		
	}

}