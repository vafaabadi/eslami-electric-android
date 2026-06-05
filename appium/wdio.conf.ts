import 'dotenv/config';
import path from 'node:path';
import type { Options } from '@wdio/types';

const appApk = process.env.APP_APK_PATH
  ? path.resolve(__dirname, process.env.APP_APK_PATH)
  : path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk');

export const config: Options.Testrunner = {
  runner: 'local',
  hostname: process.env.APPIUM_HOST || '127.0.0.1',
  port: Number(process.env.APPIUM_PORT || 4723),
  specs: ['./specs/**/*.spec.ts'],
  exclude: [],
  maxInstances: 1,
  capabilities: [
    {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': process.env.ANDROID_DEVICE_NAME || 'Pixel_7_API_34',
      'appium:platformVersion': process.env.ANDROID_PLATFORM_VERSION || '14',
      'appium:appPackage': 'com.eslamielectric.android',
      'appium:appActivity': 'com.eslamielectric.android.MainActivity',
      'appium:app': appApk,
      'appium:autoGrantPermissions': true,
      'appium:noReset': process.env.APP_REINSTALL !== 'true',
      'appium:fullReset': process.env.APP_REINSTALL === 'true',
      'appium:newCommandTimeout': 180,
      'appium:adbExecTimeout': 120000,
    },
  ],
  logLevel: 'info',
  bail: 0,
  waitforTimeout: 15_000,
  connectionRetryTimeout: 120_000,
  connectionRetryCount: 2,
  services: [
    [
      'appium',
      {
        command: 'appium',
        args: {
          relaxedSecurity: true,
        },
      },
    ],
  ],
  framework: 'mocha',
  reporters: ['spec'],
  mochaOpts: {
    ui: 'bdd',
    timeout: 120_000,
  },
};
