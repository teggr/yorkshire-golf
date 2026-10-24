# yorkshire-golf

Web based tracker for Yorkshire Golf

# Copilot Instructions

For consistent naming of pages/sections and guidance for the `scripts` Course Audit tool, see [.github/copilot-instructions.md](.github/copilot-instructions.md).

# Data Store

Managed as resouces:

* `courses.csv` contains the list of yorkshire courses
* `course-records.csv` contains the list of played courses. fk into the courses list

# Build

Requires:

* Java 21
* Maven

```shell
./mvnw clean package
```

# Release

```shell
./mvnw versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false

./mvnw clean package 

./mvnw docker:push
```

# Deploy

Build the Docker image:

```shell
deploy4j deploy --version=0.0.15
```

# Post-Deploy

```shell
./mvnw versions:set -DnextSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```

# Usage

Go to https://www.yorkshiregolf.life

# Course Audit

Run from repo root:

```shell
jbang scripts/CourseAudit.java
```

By default, closed courses are hidden.

To include closed courses for editing:

```shell
jbang scripts/CourseAudit.java --show-closed
```


# Useful References

https://www.chartjs.org/docs/latest/samples/other-charts/multi-series-pie.html
https://stackoverflow.com/questions/20966817/how-to-add-text-inside-the-doughnut-chart-using-chart-js

# Federated login — Phase 1

## Existing authentication architecture

The application is a single Maven module using Java 21, Spring Boot 4.0.3 and
Spring Security 7.0.3. Spring MVC controllers render j2html views. JDBC repositories
use a file-backed H2 database; Liquibase applies additive YAML changelogs at startup.
There is no separate identity service.

Existing accounts have a numeric `golf_user.id`, unique email, BCrypt password,
generated public `tracker_id`, role and lockout counters. There is no separate
public username to collect. Rounds reference the numeric user ID; public Challenge
Tracker URLs use the tracker ID. Registration, form login, POST logout and
hashed, expiring password-reset tokens remain available. Successful login returns
to `/challenge`. Browser actions use Spring Security CSRF protection.

External identities are separate from local accounts: only the provider and its
opaque, case-sensitive subject identify a provider login. Matching emails never
merge accounts. To use a provider with an existing account, sign in to that account
first and explicitly link the provider. Existing IDs, tracker URLs and round
ownership must not change. Provider-only accounts do not need an email or password.

Local login continues to identify existing accounts by email; federated sessions
instead carry the local numeric user ID, not provider claims. The identity table
stores no names, emails, pictures or provider tokens. OAuth authorized clients
are not retained for API access or refresh.

## Configuration

All providers default to disabled and can be enabled independently. Supply the
following environment variables through the existing deployment environment:

| Provider | Enable flag | Client ID | Client secret |
| --- | --- | --- | --- |
| Google | `AUTH_GOOGLE_ENABLED=true` | `AUTH_GOOGLE_CLIENT_ID` | `AUTH_GOOGLE_CLIENT_SECRET` |
| Apple | `AUTH_APPLE_ENABLED=true` | `AUTH_APPLE_CLIENT_ID` | `AUTH_APPLE_CLIENT_SECRET` |
| Facebook | `AUTH_FACEBOOK_ENABLED=true` | `AUTH_FACEBOOK_CLIENT_ID` | `AUTH_FACEBOOK_CLIENT_SECRET` |

Apple's secret is the signed JWT described below, not the downloaded private key.
An enabled provider requires both credentials; malformed configuration fails
startup without printing secret values. The underlying Spring properties are
`golf.auth.<provider>.enabled`, `.client-id` and `.client-secret`.
`APP_BASE_URL` supplies the trusted public origin used for callbacks: HTTPS in
production, or HTTP loopback for local Google/Facebook development where accepted
by the provider. Restart after changing credentials or enable flags.

Visit `/account` while signed in to manage sign-in methods. Local accounts confirm
changes using their current password. Accounts without a password first confirm
through a new authorization with an already-linked provider. Apple can reuse its
own SSO session; this is proof of control, not a guarantee of a new password prompt.
Linking starts a new provider authorization
flow; it does not replace another link or match accounts by email. Removing the
last usable sign-in method is rejected, including when other linked providers
are disabled. Sensitive operations are rate-limited.

## Provider console setup

Use separate applications and credentials for development/staging and production.
Register the exact callback URL, including scheme, host and path:

* Google: `https://yorkshiregolf.life/login/oauth2/code/google`
* Apple: `https://yorkshiregolf.life/login/oauth2/code/apple`
* Facebook: `https://yorkshiregolf.life/login/oauth2/code/facebook`

If using `www` or a staging hostname, register that exact hostname instead and
set `APP_BASE_URL` consistently. Do not use wildcard callbacks or derive public
callbacks from an untrusted Host header.

**Google:** In [Google Auth Platform](https://console.cloud.google.com/auth/overview),
configure the consent screen, audience/test users and a Web application OAuth
client. Add the callback as an authorized redirect URI. Authentication needs only
`openid`, not Gmail, Contacts or other Google APIs.

**Apple:** Follow Apple's
[web configuration guide](https://developer.apple.com/help/account/capabilities/configure-sign-in-with-apple-for-the-web/).
Enable Sign in with Apple for a primary App ID, associate a Services ID with it,
and configure the website domain and exact return URL. The Services ID is the
client ID. Create a Sign in with Apple key and use it to generate an ES256-signed
client-secret JWT with the Team ID, Key ID, Services ID and Apple audience,
following Apple's
[token documentation](https://developer.apple.com/documentation/signinwithapplerestapi/generate-and-validate-tokens).
Keep the signing key outside the repository. Client secrets expire after at most
six months; schedule rotation before expiry and replace the deployed secret.
Apple requires an eligible Apple Developer Program account, which can entail a
membership fee; this implementation does not remove that provider prerequisite.
Web return URLs require a real HTTPS domain, not localhost or an IP address.
Only `openid` is requested, avoiding name/email scopes. Apple uses a cross-site
`form_post` callback. Its exact POST callback is exempt from the application's
CSRF-token check, but must pass Spring Security's session-bound OAuth state and
OIDC nonce validation. Other browser POST actions retain CSRF protection.
When Apple is enabled, session cookies require `SameSite=None; Secure` so the
callback can recover its original session; otherwise cookies use `SameSite=Lax`.

**Facebook:** Create a suitable application in
[Meta for Developers](https://developers.facebook.com/apps/), enable Facebook
Login for the web, configure the site domain and Valid OAuth Redirect URI, and
obtain the App ID and App Secret. Use development-mode test users before publishing.
Only the application-scoped user ID is needed; do not request email or retrieve
names/photos. Check Meta's current publishing, privacy-policy and data-deletion
requirements before enabling public login.

Never place credentials, authorization codes, access/ID tokens, raw provider
responses or provider email addresses into logs, screenshots, support tickets or
source control. Disable HTTP wire/TRACE debugging in deployed environments and
avoid recording callback query strings in proxy/access logs.

Real-provider smoke tests require operator-supplied, non-production credentials
and provider-console configuration. Deterministic automated tests do not replace
these checks, provider approval or a review of current branding requirements.

## Automated verification

With Java 21 selected, run the focused authentication tests, then the full suite:

```shell
./mvnw test -Dtest=FederatedProvidersTest,FederatedSecurityTest,OidcProtocolTest,AccountActionGuardTest,ExternalIdentityServiceTest,UserRepositoryTest
./mvnw test
```

Protocol tests use a local mock provider and signed test ID tokens, not live
credentials. They exercise the real Spring Security callback processing,
including invalid token claims, nonce/state rejection and session rotation.

## Deployment and rollback checklist

* Back up the existing H2 database using the existing operational process before
  applying the migration. Test the migration against a copy first.
* Deploy initially with all providers disabled. Liquibase applies the additive
  identity mapping; retain all existing account data.
* Configure credentials through the deployment secret store, not tracked files.
  Deploy4j currently passes secrets listed under `env.secret` in its deployment
  configuration; add only the enabled providers' variable names there and supply
  their values through the existing secret mechanism.
* Production is behind Traefik, with TLS on `websecure` and HTTP redirected to
  HTTPS. Set `APP_BASE_URL=https://yorkshiregolf.life`; ensure the backend is not
  publicly reachable around the trusted proxy. Check external callback URLs and
  Secure, HttpOnly and SameSite session-cookie attributes in a browser.
* Enable and smoke-test providers individually on staging, then production:
  first login, repeat login, explicit linking to an existing account, rejection of
  an already-owned identity, cancellation, unlink protection and POST logout.
  Verify existing email/password login, password reset and round ownership.
* Monitor generic authentication failures and unexpected account creation without
  logging personal claims or tokens. Verify Apple secret rotation operationally.
* To roll back, disable the affected provider and redeploy/restart. Keep local
  email/password login available. Do not delete users or identity mappings.
* Before rolling back to an older application binary, assess compatibility with
  provider-only accounts whose email/password are absent. Prefer disabling
  providers over reverting the schema or deploying an incompatible binary.

Provider-only accounts have no email-based recovery unless a separate local login
method is deliberately added. Link another usable provider before losing access
to the only one. Recovery redesign, password retirement and deletion of existing
personal data are outside Phase 1.
