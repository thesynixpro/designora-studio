# Designora Studio

Designora Studio is an Android-native, AI-assisted design workspace built with Kotlin and Jetpack Compose. It is a functional foundation for creating and editing structured UI design documents rather than generating a single flattened image.

## Technology

- Kotlin 2.0.21, Android Gradle Plugin 8.6.1, Gradle 8.7, JDK 17
- Jetpack Compose + Material 3 and Navigation Compose
- Room for local project persistence
- AndroidX Security Crypto for encrypted provider credentials
- OkHttp for Gemini and OpenRouter APIs
- Bundled Lucide-style SVG icon pack rendered with AndroidSVG (no emoji-based UI icons)
- Coroutines for network and database work

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
gradle --no-daemon :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Current implemented workflow

1. Create a project with a name, category, style and natural-language brief.
2. Configure a Gemini or OpenRouter API key in AI Providers. Credentials are stored with AndroidX encrypted preferences and are not included in project JSON.
3. Fetch the provider's actual model list from its API, select a model, and test the connection.
4. Generate a structured JSON design document through the selected provider.
5. Generated design prompts explicitly prohibit emoji and request named SVG icons from the bundled icon pack; accidental emoji in AI copy is sanitized before saving.
6. Reopen saved projects from the Room database.
7. Edit element text and font size, add text elements, duplicate or delete selected elements, and save changes.
8. Start from clearly labelled built-in templates.
9. Export the first page as an HTML document using Android's document picker.

## Important scope notes

This is a working first implementation, not a claim that every feature in the larger product brief is complete. The current editor is a touch-friendly structured canvas with basic element editing; full freeform drag/resize, multi-select, collaborative editing, image understanding, SVG/PNG/PDF rendering, React export, user accounts/cloud sync, marketplace submissions, and full version history are not implemented yet. API image inputs are not sent to the model. These should be added in tested milestones rather than represented by inactive controls.

## API setup

- Gemini: create an API key from Google AI Studio and select the Gemini provider.
- OpenRouter: create an API key from OpenRouter and select OpenRouter.
- Use **Fetch models** to retrieve live models. The app does not present a hardcoded model catalogue as fetched data.

Never share your API keys or commit them to the repository. Keys are stored locally using AndroidX Security Crypto.

## GitHub Actions

`.github/workflows/android.yml` builds the debug APK, uploads it as an Actions artifact, and publishes it as a GitHub Release for pushes to `main` or `master`. Pull requests build the APK but do not publish a release.


## Visual direction and icon policy

The interface uses a restrained warm-neutral product aesthetic, compact corner radii, consistent spacing, charcoal typography, and a single controlled accent. The app UI uses vector icons, and generated design instructions prohibit emoji or Unicode pictograms. A bundled Lucide-style SVG set lives in `app/src/main/assets/icons/`; generated `iconName` values are restricted to this allowlist and rendered with AndroidSVG. AI-provided text is sanitized to remove accidental emoji before the design is persisted.

## Launcher icon

The Android launcher icon is implemented as a custom Designora `D` monogram. It includes:
- Adaptive launcher icons for Android 8.0+ (`mipmap-anydpi-v26`), including a monochrome layer for supported themed icons.
- Density-specific PNG fallbacks for older Android versions.
- A transparent vector foreground and a separate deep-charcoal background.
- Manifest wiring through `android:icon` and `android:roundIcon`.

The editable vector source is `app/src/main/res/drawable/ic_launcher_foreground.xml`; the original SVG artwork is `app/src/main/assets/designora-app-icon.svg`.

## Custom AI instructions

Open **Settings → My AI instructions → Add instructions** to write your own design rules. Save them once; they are stored locally on the device and automatically appended to future design-generation requests sent through either Gemini or OpenRouter. You can edit or clear the instructions at any time. The app keeps its required JSON response schema and built-in safety/output constraints.
