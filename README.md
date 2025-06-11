# rnb-android

An Android app which displays light novels in the `.rnb` file format created by
[rnb](https://github.com/rsookram/rnb).

Besides displaying the content of a book, there are few features other than
saving reading progress (which is only for the most recently opened book). I
tried to make the app small, so there are no third-party dependencies, and it's
this small:

- `.apk`: 28,932 B
- `classes.dex`: 18,248 B
- `classes.oat`: 43,048 B (when AOT with `dex2oat`)

## How to use

This app expects `.rnb` files to be present in the `books/` directory within
external storage. This is usually `/sdcard/books/`.

## Building

Run the following command from the root of the repository to make a debug
build:

```shell
./gradlew assembleDebug
```

Making a release build is similar, but requires environment variables to be set
to indicate how to sign the APK:

```shell
STORE_FILE='...' STORE_PASSWORD='...' KEY_ALIAS='...' KEY_PASSWORD='...' ./gradlew assembleRelease
```

## Permissions

`android.permission.MANAGE_EXTERNAL_STORAGE` is used to access files on
external storage. This app doesn't have any other permissions.
