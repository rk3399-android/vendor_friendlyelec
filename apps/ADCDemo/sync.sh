#!/bin/bash

adb disable-verity; adb root; adb remount; adb sync; adb reboot;
