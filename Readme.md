
- start point to discover: `main.processEvents()`


- error hierarchy `src/main/scala/domain/Error.scala`


- domain models `src/main/scala/domain/Event.scala`


- `MatchStateAccumulator` - container which hold `matchState` and `inconsistentEvents`

  `MatchStateAccumulator.matchState.latest()` - latest Event

  `MatchStateAccumulator.matchState.latest(n: Int)` - latest n Events

  `MatchStateAccumulator.matchState.allEvents()`   - all valid events

  `MatchStateAccumulator.matchState.+(event: Event)` - add an Event

  `MatchStateAccumulator.inconsistentEvents`     - all inconsistency events collected during processing



- run `main()` and all valid events which are going to be produced down to the stream is going to be `printed`
into the console in `json format`.

     
- `val matchStateSample1` and `val matchStateSample2` - holds match states based on events consumed from the corresponding files`.simple1.txt` and `simple2.txt`.
`simple1.txt` and `simple2.txt` lives under `src/main/resources`


