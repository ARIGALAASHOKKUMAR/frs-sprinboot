frs-sprinboot

## Deploy on Render with Docker

Create a Render Web Service from this repository and select the Docker runtime. Render detects the root [Dockerfile](./Dockerfile) and builds the application image.

In Render's Environment settings, add `DATABASE_URL` and paste the PostgreSQL Internal Database URL from your Render database. Render supplies `PORT` automatically; do not set it manually. Use `/` as the health check path.

For local development, copy [.env.example](./.env.example) to `.env` and fill in your local database values. Never commit `.env`; `.dockerignore` excludes environment files from the Docker build context.
