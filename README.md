To use TSA for Tournaments of your own, you have to change 4 files:
1. Create src/main/resources/token.txt. Add the following credentials, one per line:
  - Lichess token
  - Bluesky token
  - X Api Key, X Api Key Secret, X Access Token, X Access Token Secret
2. Change the Lichess and Bluesky user names in src/main/scala/TournamentAdmin.scala   
3. Enter your own series in series.json
4. Enter the last created instance of each series in instances.json
Recompile, copy Jar, create Timer, ready :-)
