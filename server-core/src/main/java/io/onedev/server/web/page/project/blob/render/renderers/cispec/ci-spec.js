onedev.server.ciSpec = {
    onDomReady: function(selection, callback) {
        var activeJobIndex = -1; 
        var $body = $(".ci-spec>.valid>.jobs>.body");
        $body.data("callback", callback);

        $body.find(">.contents>.content").each(function() {
            var $this = $(this);
            if ($this.find(".feedbackPanelERROR").length != 0) {
                activeJobIndex = $this.index();
                return false;
            }
        });
        if (activeJobIndex == -1) {
            if (selection && selection.startsWith("jobs/")) {
                var activeJobName = selection.substring("jobs/".length);
                activeJobIndex = $body.find(">.side>.navs>.nav[data-name='" + activeJobName.escape() + "']").index();
            }
        }
        if (activeJobIndex == -1)
            activeJobIndex = 0;

        onedev.server.ciSpec.showJob(activeJobIndex);

        $body.find(".side>.navs>.nav>.select").click(onedev.server.ciSpec.selectJob);
    },
    selectJob: function() {
        var $nav = $(this).parent();
        onedev.server.ciSpec.showJob($nav.index());
        var $body = $(".ci-spec>.valid>.jobs>.body");
        if ($body.data("callback"))
            $body.data("callback")("jobs/" + $nav.data("name"));
    },
    showJob: function(index) {
        var $body = $(".ci-spec>.valid>.jobs>.body");
        var $navs = $body.find(">.side>.navs");
        $navs.children().removeClass("active");
        var $nav = $navs.children().eq(index);
        $nav.addClass("active");
        var $contents = $body.children(".contents");
        $contents.children().hide();
        var $content = $contents.children().eq(index);
        $content.show();
        onedev.server.focus.doFocus($content);
    },
    edit: {
        deleteJob: function(index) {
            var $body = $(".ci-spec-edit .jobs>.body");
            var $navs = $body.find(">.side>.navs");
            var $contents = $body.children(".contents");

            var $nav = $navs.children().eq(index);
            $nav.remove();
            $contents.children().eq(index).remove();

            if ($nav.hasClass("active")) 
                onedev.server.ciSpec.showJob(0);
        }, 
        swapJobs: function(index1, index2) {
            var $contents = $(".ci-spec-edit .jobs>.body>.contents");

            if (index1 < index2) {
                for (var i = 0; i < index2-index1; i++) 
                    $contents.children().eq(index1+i).before($contents.children().eq(index1+i+1));
            } else {
                for (var i = 0; i < index1-index2; i++) 
                    $contents.children().eq(index1-i).after($contents.children().eq(index1-i-1));
            }
        },
        trackJobNameChange: function(index) {
            var $body = $(".ci-spec-edit .jobs>.body");
            var $navs = $body.find(">.side>.navs");
            var $nav = $navs.children().eq(index);
            var $contents = $body.children(".contents");
            var $content = $contents.children().eq(index);
            
            var $input = $content.find(">div>table>tbody>tr>td.property-name input");

            function syncName() {
                var name = $input.val().trim();
                var $name = $nav.find("a.select>.name");
                if (name.length != 0) 
                    $name.text(name);
                else
                    $name.html("<i>Name not specified</i>");
            }

            $input.on("input", syncName);
            
            syncName();
        }
    }
}