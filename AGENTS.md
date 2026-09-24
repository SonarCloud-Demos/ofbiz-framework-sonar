Development rules

We maintain 80% code coverage on newly introduced or modified code. Do your best to reach this level before pushing code. You can use `sonar qg status --category coverage` to get details on the coverage omce the check is done.
When introducing a new third-party dependency, make sure to immediately build and then run `sonar analyze dependency-risks --format toon` and take steps to correct the risks if any.