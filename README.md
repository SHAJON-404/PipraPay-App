# PipraPay Android Gateway App

PipraPay-App is an open-source Android application designed to act as an SMS gateway. It forwards incoming SMS messages to configured webhook URLs. In addition, it supports a periodic keep-alive/heartbeat check to verify that both the device and webhook endpoints are online and active.

---

## 🚀 Key Features

* **SMS Forwarding**: Automatically capture incoming SMS messages and forward them to your server endpoints (Webhooks).
* **Multi-SIM Support**: Select a specific SIM slot (SIM 1, SIM 2, or Any) for each webhook configuration.
* **Custom Headers & Templates**: Configure custom headers (e.g., API keys, User-Agents) and customize the JSON payload template for forwarding.
* **Heartbeat / Keep-Alive Ping**: Periodically sends a ping payload to check the connectivity of the webhook endpoint (Configurable 1-10 min interval, default: 2 min). *(See [Heartbeat Payload](#2-periodic-keep-alive--heartbeat-request) for format).*
* **Battery Saver / Auto-Shutdown**: Automatically stops the background service if no heartbeat pings reach any enabled webhooks within the selected duration (30 min - 3 hr) to prevent battery drain.
* **Activity Logs Table Console**: A dark-themed, monospaced log viewer tab showing real-time logs for SMS receptions, forwarding results, ping operations, and automatic shutdowns in a clean text-based table format. It maps long webhook URLs to compact sequential IDs (e.g., `WH1`, `WH2`) to keep the console clean and readable. **The log console prevents line wrapping and supports both horizontal and vertical scrolling.**
* **Numbered Webhook URLs**: Displays webhooks with sequential number indicators (`1.`, `2.`, `3.`, etc.) on the Home screen list for clear tracking, and automatically disables word wrapping to maintain a clean layout.
* **Context-Aware Add Button**: The floating `+` button for adding new webhook configurations is context-sensitive. It is only displayed when viewing the main webhook list (Home page) and automatically hidden on the Logs and Settings tabs to keep the interface clean.
* **Export Logs File**: Includes a dedicated "Export Logs" button to save logs directly to the public `Downloads/PipraPay/` directory (automatically creating the folder if it does not exist) with filenames using the template: `piprapay-{day-month-year}-{timestamp}.txt`.
* **Background Reliability**: Excludes battery optimizations and starts automatically on boot (if enabled) to keep the app active in the background.

---

## 🛠️ How It Works

1. **Foreground Service (`SmsReceiverService`)**: Keeps the app active in memory, registering a broadcast receiver to listen for `SMS_RECEIVED_ACTION`.
2. **WorkManager (`RequestWorker`)**: Standard Android utility for queuing network requests with retry policies and network constraints, ensuring delivery.
3. **Periodic Webhook Heartbeat**: Pings configured webhook URLs with a keep-alive message at custom intervals defined in the UI settings.

---

## 📡 Webhook Payloads & Formats (A-Z Details)

PipraPay-App sends two distinct types of requests to your configured webhook URLs:

### 1. SMS Forwarding Request
* **Trigger**: Fired immediately when an incoming SMS message matches a webhook's rule (sender filter).
* **HTTP Method**: `POST`
* **Content-Type**: `application/json; charset=utf-8`
* **Custom Headers**: Any custom HTTP headers configured in the webhook's edit settings.
* **Payload Variables**:
  * `%from%`: The originating phone number (sender).
  * `%text%`: The text body of the SMS message.
  * `%sentStamp%`: The Unix timestamp when the SMS was sent from the sender (in milliseconds).
  * `%receivedStamp%`: The Unix timestamp when the SMS was received by the Android device (in milliseconds).
  * `%sim%`: The SIM slot that received the message (`sim1`, `sim2`, or `undetected`).
* **Default JSON Payload Structure**:
  ```json
  {
    "from": "%from%",
    "text": "%text%",
    "sentStamp": %sentStamp%,
    "receivedStamp": %receivedStamp%,
    "sim": "%sim%"
  }
  ```

---

### 2. Periodic Keep-Alive / Heartbeat Request
* **Trigger**: Executed periodically based on the interval (minutes) configured in the app settings.
* **HTTP Method**: `POST`
* **Content-Type**: `application/json; charset=utf-8`
* **Custom Headers**: Automatically inherits the custom headers configured for each webhook (e.g., Authorization headers, API keys).
* **Payload Format**:
  ```json
  {
    "success": true,
    "time_stamp": 1764502800
  }
  ```
  * `success` (Boolean): Always set to `true`.
  * `time_stamp` (Long): The current Unix epoch timestamp (in **seconds**) representing when the heartbeat check was dispatched.

---

## ⚙️ Connection & HTTP Transport Specifications

* **TLS / SSL Customization**: The application includes a custom `TLSSocketFactory` to force secure handshakes. You can toggle **Ignore SSL** in a webhook's settings to bypass validation for self-signed certificates (testing/development).
* **Chunked Transfer Encoding**: By default, requests are sent using `ChunkedStreamingMode` (`Transfer-Encoding: chunked`). This can be toggled off inside the webhook config screen to fall back to fixed length requests (`Content-Length`).
* **Network Retry Policy**: Failed requests (e.g., due to temporary network loss or server timeouts) are queued and retried automatically. The maximum number of retry attempts is customizable per webhook (default: `10`).

---

## 💻 Tech Stack & Requirements

* **Language**: Java
* **IDE**: Android Studio
* **Build System**: Gradle
* **Libraries**:
  * **Volley**: For making simple periodic network checks.
  * **WorkManager**: For reliable, constraint-based network execution.
  * **Material Components**: For design and UX elements.

---

## ⚙️ Installation & Build

1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/PipraPay.git
   ```
2. Open the `PipraPay-App` folder in Android Studio.
3. Allow Gradle synchronization and dependencies download.
4. Connect an Android device (or emulator with cellular simulation) and run the project:
   * Build APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`

---

## 📱 Configuring Settings from the UI

### 1. Heartbeat Interval
* Navigate to the **Settings** screen (Gear icon at the bottom).
* Locate the **Ping Interval** card under General settings.
* Select your desired interval (from 1 to 10 minutes) using the dropdown spinner.
* The setting is saved immediately and automatically updates the heartbeat task interval on the background service. (Make sure background worker is running by enabling **Start PipraPay** from the Home tab).

### 2. Auto Service Stop (Battery Saver)
* Navigate to the **Settings** screen.
* Locate the **Auto Service Stop** card directly below the **Ping Interval** settings card.
* Select your desired auto-shutdown delay interval (options: `30 min`, `1 hr`, `1.5 hr`, `2 hr`, `2.5 hr`, `3 hr`) from the dropdown spinner (default: **30 min**).
* If the background service is running and fails to successfully ping any of the enabled webhook URLs continuously for the selected duration, the service will automatically stop itself to preserve battery.

---

## 💖 Credits & Acknowledgments

This application is a rewritten and optimized version of the original **[PipraPay-V2-App](https://github.com/PipraPay/PipraPay-V2-App.git)** repository.

We would like to express our deepest gratitude and appreciation to the original developers of the PipraPay-V2-App project. Their open-source contribution and foundational architecture served as the key blueprint for making this modern rewrite possible. Thank you!
