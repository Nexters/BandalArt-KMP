fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android internal

```sh
[bundle exec] fastlane android internal
```

Build, validate, and upload a production-ad release AAB to Play Internal Testing

----


## iOS

### ios preflight

```sh
[bundle exec] fastlane ios preflight
```

Verify the Individual App Store Connect key and report the next TestFlight build

### ios beta

```sh
[bundle exec] fastlane ios beta
```

Archive the iOS app with manual signing and upload the next build to TestFlight

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
