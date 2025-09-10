# spring-telegram-posting

## Telegram Publisher Service

A small service that accepts an image + caption and posts to a Telegram channel.

## Prerequisites
1. Create a Telegram **Bot** via @BotFather and obtain the **bot token**.
2. Add the bot to your channel and promote it to **Admin** with permission to post messages.
3. Determine your **chat id**:
   - If the channel has a public username, you can use `@your_channel_username`.
   - Or use the numeric `-100xxxxxxxxxx` chat id.

## Configuration
Environment variables:
- `TELEGRAM_BOT_TOKEN` — token from @BotFather
- `TELEGRAM_CHAT_ID` — target channel (e.g., `@my_channel` or `-1001234567890`)
- `PUBLISH_AUTH_TOKEN` — optional bearer token enforced by the service

You can also set these via `application.yml`, but env vars are recommended for secrets.

## Run (Maven)
```bash
mvn spring-boot:run