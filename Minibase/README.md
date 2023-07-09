logic for extracting join conditions: I first split all body atoms to relational 
atom list and comparison atom list. And for the comparison atom list, I have a function
splitCompare() in QueryBuilder class to split selection condition and join condition.
If a comparison atom's terms involved in 1 or more relational atoms, I add this atom to join condition list,
otherwise add to selection atom list. Then the generated join condition list will be the candidate
join conditions for join. And for each join, I will loop through these candidates to check
if any of them is suitable for this join. If both atom of this join contains at least 1 variable
that appear in this join comparison, then this comparison is suitable, and it will be added to this join's
restriction list. More details about join operation can be find in joinOperator's class comment(line11) 
and mode details about left-deep join tree generation and plan generation can be found by reading QueryBuilder's comment.

Optimisation rules: Details about QueryBuilder's implementation can be found in its comments.
Here I just talks some main optimisation operations. 1: For each relation, I will find all its suitable 
selection conditions and apply it by using a SelectOperator at very beginning, so later operations
can have less input tuples. 2: If the query do not output SUM(), then there is no need to keep duplicate,
so in this case, I will add a project operator after temporary root operator to remove duplicates, 
and this project will only keep necessary variable(those variables appeared in comparison atoms or head output).
This reduces the number of intermediate tuples as much as possible while keeping the result the same.
But I do not apply it for every temporary root, since the project operator itself will have buffer to store
appeared tuples, which means it will cost many space if there are many projection operators, So in my design
I let this projection only added when current root's has at least 2 more output variables than necessary variables,
to make it only simplify the temporary root when it is complicated enough.