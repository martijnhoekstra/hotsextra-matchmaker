# A heap based matchmaker exploration

This is an exploration for how matchmaking could work in Heroes of the Storm. A live view of the matchmaker can be played with at https://lit-bayou-1819.herokuapp.com/matchmaking

## What is this?

This is a matchmaking algorithm for online games in general, and Heroes of the Storm in particular.
It consists of a matchmaking algorithm, and a web interface to test it. Initial runs show it's very efficient, running easily on a Heroku free instance, even with hundereds of simulated joins per second.
In this implementation, a pool of a fixed number of players (as Blizzard hinted they do with fixed pools of 100 players) isn't needed; It effortly scales due to the efficient datastructures used.

## How does it work?

The matchmaker pulls a sliding window of 10 players over the queue of players, who are ordered by their ranking in a min-heap. New players go on the heap as they arrive.
If a fitness function decides the match is "good enough", the 10 players are divided in two teams by a match building function.
Both functions can be declared and passed to the matchmaker independently.

## Why do this?

I wrote this matchmaker primarily to see how a matchmaker works. There is also a certain amount of "putting my money where my mouth is". If I am to claim that Blizzards matchmaker doesn't perform as well as it could, then surely, I could do better. This is an attempt to demonstrate it can be done better.

## What works, and what doesn't work?

Currently, the only thing fully implemented is hero league solo queue. This is by far the easiest to implement, because it doesn't have the additional requirements of team composition, and doesn't have to worry about team size.

Next up are group queue (teams of size 2 or 3) hero league. Then Quickmatch. Team league comes last, because it's a very simple problem comperitatively, and I can't imagine Blizzards team league matchmaker has any problems.

## Why doesn't your matchmaker do X instead of Y?

Good question! Open an issue to discuss it. If we agree, I can see if I can implement it, or you can submit a pull request yourself, and we can see how it performs.

## Cool stuff! How can I help?

In any way you want to. The web interface for testing is really shakey. Actually, the whole web side of things is really bad all-round. If you're good with html/css, you could already make that a ton better. If you are handy with packaging Scala applications, the static files should be delivered statically and have proper caching headers. If you have good ideas on how things should work, you can express them in an issue. If you have see I'm doing things terribly wrong, you can yell at me for being stupid and doing it wrong (and show me how to do it right). If you think I'm taking this in a whole wrong direction, you can fork the project, and take it back to the right direction. If you do any of those things, you have my undying gratitude.

## Dude, you're doing a hobby project in your spare time. Blizzard has tons of the best engineers money can buy. Do you seriously think you can do better than them?

Maybe.
