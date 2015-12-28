HotsExtra Matchmaking is a project exploring solutions to the difficuties in matchmaking around Heroes of the Strom

Complaining about matchmaking is a popular passtime for Heroes of the Storm players. A cynic might remark it seems even more popular a passtime than playing Heroes of the Strom. But there is a decent reason people complain so much; matchmaking a game like Heroes of the storm is very tricky.

The role of ranking and matchmaking in Heroes of the Storm is crucial to the game experience. Games that have players with skill levels close to each other are found to be more enjoyable than those with a large diffence in skill levels. This goes both for the skill levels within a team, as for the general skill level between the opposing teams. It is the matchmakers task to find games that are enjoyable. It should do this in a reasonable amount of time.

Other than for matchmaking, it has been shown that players enjoy being able to see their skill level in relation to other, and seeing an estimation of their skill level incentivises players to take actions that increases this metric.

We currently don't know much about the ranking and matchmaking algorithm that Blizzard employs. Therefor, this project exists in a bit of a vaccuum. How does one demonstrate they do better, when they can't demonstrate what another does? We attempt to solve this by looking at the data there is available, and attempting to draw conclusions from there. Ofcourse, we're  not entirely in a vaccuum. A vast body of work has been written about matchmaking. ELO is one of the oldest approaches to estimate skill, and has existed since x. The Glicko and Glicko-2 systems have improved upon ELO by quantifying the uncertainty in skill and the volatitily of skill respectively. Microsoft has created the TrueRank ranking system which further improves on these rating systems by estimating individual skill in team based games.

There are several game modes in Heroes of the Storm, and each has its own challenges in matchmaking. The game modes are

* Team league, where one fixed team plays against another fixed team
* Hero league, where 5 players are matches against 5 other players, and heroes are picked by the players once the team is formed
* Quick match, where 5 players are matched against 5 other players, and the heroes are picked in advance.

Of these, team league is the easiest to model, because it reduces the problem to a 1v1 scenario. Because of this, any existing ranking and matchmaking scheme for 1v1 can be used. This is a well understood problem.

Hero league let's players for a team by choosing a character (hero). Because people may have different skill levels with different heroes, and heroes are only chosen after the matchmaker picks the teams, a large uncertainty is entered in to matchmaking.

Quick match has a set of constraints that limit which heroes can be matched against each other to make a rounded team. This increases the search space the matchmaker has to consider.

Selecting the optimal teams from a pool of players to make equal stength teams in general reduces to the 1D knapsack problem which is known to be NP-hard. A closed form optimal solution is out of reach. 