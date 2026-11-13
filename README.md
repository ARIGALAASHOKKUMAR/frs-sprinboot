frs-sprinboot

## Deploy on Render with Docker

Create a Render Web Service from this repository and select the Docker runtime. Render detects the root [Dockerfile](./Dockerfile) and builds the application image.

Set `DATABASE_URL` in the Render service's Environment settings. Do not commit database credentials or `.env` files. Render supplies `PORT` automatically, and the application listens on that port. Use `/` as the health check path.
