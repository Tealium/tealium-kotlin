# TagManagement Dispatcher - Change Log

## 1.3.1 - Mar 2026
- BugFix: WebView `console.log()` messages are now always intercepted by `WebChromeClient`, preventing `I/chromium` logs from appearing in logcat regardless of `LogLevel` setting

## 1.3.0 - Dec 2025
- `WebView` updates
  - Breaks up init code into smaller blocks to assist in alleviating some ANRs
  - Added `WebViewInitPolicy` to allow more flexibility around when the TagManagement WebView initializes
    - Default remains `Immediate` which will be behaviourally the same as previous releases
- Disables WebView console logs by default; can be enabled by new configuration property `TealiumConfig.webViewLogsEnabled`

## 1.2.2 - May 2024
- WebView is refreshed on new session to ensure latest IQ configuration is available

## 1.2.0 - Oct 2022
- `QueryParameterProvider` - allows additional parameters to be added to the URL used for the TagManagement module
- Improved WebView instantiation and page load management

## 1.1.3 - Sep 2022
- BugFix: Relocate `sessionCountingEnabled` to fix miscount on fresh launch

## 1.1.2 - Feb 2022
- Added `sessionCountingEnabled` to `TealiumConfig` to enable/disable session counting for TealiumIQ

## 1.1.0 - Nov 2021
- Serialization standardized to use `JsonUtils`
- Dependency updates in line with Core 1.3.0

## 1.0.7 - Oct 2021
- Event key references updated to use `Dispatch.Keys.XXX` (requires Core 1.2.8+)

## 1.0.6 - Jul 2021
- Support for Consent Logging profile override

## 1.0.4 - Mar 2021
- BugFix: Route `HttpRemoteCommand` to background thread for execution

## 1.0.3 - Mar 2021
- ProGuard and Consumer Rules revision

## 1.0.1 - Jan 2021
- ProGuard rules added to generated binaries and consumer proguard rules

## 1.0.0 - Oct 2020
- Initial stable release as part of the modular Tealium Kotlin SDK

## 0.1.0 - Aug 2020 (Beta)
- Initial beta release as part of Tealium Kotlin SDK
