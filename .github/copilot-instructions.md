# Copilot Instructions for `yorkshire-golf`

## Scope and style
- Keep changes small and focused.
- Preserve the existing stack and patterns: Java 21, Spring MVC controllers, j2html page views, resource-backed YAML data.
- Prefer existing naming and routes over introducing new terms.

## Scripts folder: Course Audit
- Canonical name: **Course Audit** (`scripts/CourseAudit.java`).
- Purpose: local audit/editor for course YAML + course images.
- Run from repo root with: `jbang scripts/CourseAudit.java`.
- Default local URL: `http://localhost:7070`.
- Key Course Audit routes/actions:
  - `GET /` current course audit screen
  - `GET /courses-list` list of all course files
  - `POST /next`, `POST /previous` navigation
  - `POST /jump-to-letter`, `POST /search-course` lookup/navigation
  - `POST /update-website`, `POST /update-stay-image`, `POST /update-closed`, `POST /update-play-and-stay`, `POST /download-image` metadata updates

## Canonical page names and routes
- **Home** (`/`) → `HomePage`
- **Courses** (`/courses`) → `CoursesPage`
- **Challenge Tracker** (`/challenge/{trackerId}`) → `RegionTrackerPage`

## Canonical section names (use these exact labels in prompts/PRs)
- Home sections:
  - **Hero**
  - **Stat strip**
  - **Explore All Courses CTA**
  - **Yorkshire Challenge feature**
  - **Courses by Region**
- Courses page sections:
  - **Page header**
  - **Course listings** (grouped by region)
- Challenge Tracker sections:
  - **Page header**
  - **Chart**
  - **Add a Round form** (visible only to logged-in tracker owner)

## Naming consistency rules
- Use **Challenge Tracker** for `/challenge` and `/challenge/{trackerId}` (avoid alternate names).
- Use **Course Audit** when referring to the script/tooling in `scripts/`.

## Testing and TDD rules
- Follow TDD for behavior changes and bug fixes: write or update a test first, confirm it fails for the expected reason, then implement the code change, then rerun tests.
- A task is not complete until relevant automated tests pass locally.
- After code changes, run focused tests first (affected package/class), then run the broader suite when practical.
- If tests cannot be run in the current environment, explicitly state this and what remains to verify.
