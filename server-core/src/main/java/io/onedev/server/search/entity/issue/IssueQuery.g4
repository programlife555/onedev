grammar IssueQuery;

query
    : WS* (criteria|All) WS* (WS OrderBy WS+ order (WS+ And WS+ order)* WS*)? EOF
    | WS* OrderBy WS+ order (WS+ And WS+ order)* WS* EOF
    | WS* EOF
    ;

criteria
    : operator=(Mine|Outstanding|Closed|SubmittedByMe)	#OperatorCriteria
    | operator=(SubmittedBy|FixedInBuild) WS+ criteriaValue=Quoted #OperatorValueCriteria
    | FixedBetween WS+ revisionCriteria WS+ And WS+ revisionCriteria #FixedBetweenCriteria
    | criteriaField=Quoted WS+ operator=(IsMe|IsEmpty) #FieldOperatorCriteria
    | criteriaField=Quoted WS+ operator=(Is|IsGreaterThan|IsLessThan|IsBefore|IsAfter|Contains) WS+ criteriaValue=Quoted #FieldOperatorValueCriteria
    | criteria WS+ And WS+ criteria #AndCriteria
    | criteria WS+ Or WS+ criteria #OrCriteria
    | Not WS* LParens WS* criteria WS* RParens #NotCriteria 
    | LParens WS* criteria WS* RParens #ParensCriteria
    ;

revisionCriteria
	: revisionType=(Build|Branch|Tag|Commit) WS+ revisionValue=Quoted 
	;
	
order
	: orderField=Quoted WS* (WS+ direction=(Asc|Desc))?
	;

Mine
	: 'mine'
	;
	
All
	: 'all'
	;
	
Outstanding
	: 'outstanding'
	;
	
Closed
	: 'closed'
	;	
	
OrderBy
    : 'order' WS+ 'by'
    ;

SubmittedBy
	: 'submitted' WS+ 'by'
	;
	
FixedInBuild
	: 'fixed' WS+ 'in' WS+ 'build'
	;							
	
FixedBetween
	: 'fixed' WS+ 'between' 
	;

SubmittedByMe
	: 'submitted' WS+ 'by' WS+ 'me'
	;
		
Is
	: 'is'
	;

IsMe
	: 'is' WS+  'me'
	;
	
Contains
	: 'contains'
	;

IsGreaterThan
	: 'is' WS+ 'greater' WS+ 'than'
	;

IsLessThan
	: 'is' WS+ 'less' WS+ 'than'
	;

IsAfter
	: 'is' WS+ 'after'
	;

IsBefore
	: 'is' WS+ 'before'
	;

IsEmpty
	: 'is' WS+ 'empty'
	;

Build
	: 'build'
	;
	
Branch
	: 'branch'
	;
	
Tag
	: 'tag'
	;
	
Commit
	: 'commit'
	;
		
And
	: 'and'
	;
	
Or
	: 'or'
	;
	
Not
	: 'not'
	;
	
Asc
	: 'asc'
	;
	
Desc
	: 'desc'
	;			
	
LParens
	: '('
	;
	
RParens
	: ')'
	;				

Quoted
    : '"' (ESCAPE|~["\\])+? '"'
    ;
	
WS
    : ' '
    ;

Identifier
	: [a-zA-Z0-9:_/\\+\-;]+
	;    

fragment
ESCAPE
    : '\\'["\\]
    ;
