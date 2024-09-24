To use TSA for Tournaments of your own, you have to add 3 files in src/main/resources/token.txt (relative to jar):
1. Create token.txt. Add the following credentials, one per line, in that order:
  - Lichess user name, Lichess token
  - Bluesky user name, Bluesky token
  - X Api Key, X Api Key Secret, X Access Token, X Access Token Secret
2. Create series.json. Enter your own series. 
   Example: [{"index":0,"number":60,"date":"2024-10-22T20:01:00.000+02:00","pointerTimes":0,"pointerDays":0}]
3. Create instances.json. Enter the last created instance of each serie.
   Example: [{"index":0,"number":60,"date":"2024-10-22T20:01:00.000+02:00","pointerTimes":0,"pointerDays":0}]
Create Timer for executing the jar, ready :-)
