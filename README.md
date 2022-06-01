# Notepad 2
![License](https://img.shields.io/github/license/Matvey24/Notepad_2)
![Android](https://img.shields.io/badge/android-5.0%2B-blue)
![Chaquopy](https://img.shields.io/badge/Chaquopy-12.0.0-blue)
![Python](https://img.shields.io/badge/python-3.8-blue)
![APK_SIZE](https://img.shields.io/github/size/Matvey24/Notepad_2/app/release/app-release.apk?label=APK-release)
![Lines]()
**Notepad 2** is application for Android, notes, which allows writing scripts on python to do some specific tasks.

For example:
* Counter - to count number of starts of script for all time.
* Switcher - to change visible notes list to another scheduled.
* Achiever - to convert selected notes into a text in json format,
  which you can transfer (not in this app) and extract.

## Installation
Install the [stable release version](https://github.com/Matvey24/Notepad_2/raw/master/app/release/app-release.apk) by downloading apk from this GitHub.

## Reinstallation
If you have the app installed, and you want to install another version of the app, your device can tell you, that no data would be deleted by reinstallation.
But if your device does not tell you this, before reinstallation copy the app data you want to save into clipboard using `api.to_json(folder)` and copying the result, so you can reinstall the app without data loss (you need to extract this using `api.from_json(text)`)
