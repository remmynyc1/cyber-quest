# Changelog

## Added
- Employee training track: 7 branching missions and 4 "spot the red flags" emails
- Phishing simulation campaigns with click/report tracking
- Manager training page: completion by module and learner, campaign results
- Content integrity tests

## Fixed (security)
- Challenge answers no longer sent to the browser
- Threat and artifact endpoints require login; threat endpoints are manager-only
- CSRF protection on, logout via POST, HTML escaping in the manager dashboard
- Password hashes removed from API responses, shared placeholder hash replaced by temp passwords
- Credentials moved to environment variables, login page no longer shows them
- X-Forwarded-For no longer trusted, flag submissions rate-limited

## Changed
- Leaderboard shows first name and last initial to employees
- Mislabeled challenges renamed (password spraying, DNS tunneling)
