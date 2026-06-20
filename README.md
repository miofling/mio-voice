# Mio Voice

Mio Voice is an Android app for turning text into expressive speech, organizing voice presets, and using AI-assisted analysis to plan multi-voice delivery.

> **Status: Alpha.** This project is under active development. Features, local data formats, and behavior may change without backward compatibility.

## Current capabilities

- Text-to-speech generation through MiniMax TTS.
- Optional text and performance analysis through an OpenAI-compatible AI service.
- Local management, playback, and export of generated audio.

## Configuration and data disclosure

Mio Voice does not include API credentials or a hosted AI service. You must configure your own service endpoints, models, and API keys in the app, and you are responsible for the terms, availability, and charges of those services.

Text, prompts, configuration metadata, and other data needed to fulfill a request may be sent to the MiniMax or OpenAI-compatible third-party services that you configure. Review those providers' privacy policies before sending sensitive or confidential content.

## Known limitations

- Alpha releases may contain bugs and may introduce breaking changes or local-data migrations.
- Android 8.0 (API 26) or newer is required.
- TTS support currently targets MiniMax; other TTS providers are not implemented.
- OpenAI-compatible analysis depends on the selected provider's API compatibility and model behavior.
- Generation and analysis require network access, valid user-supplied credentials, and available third-party quota.
- No prebuilt APK is published in this repository; build the app locally with Android Studio/JDK 17.

## License

Licensed under the [MIT License](LICENSE).
