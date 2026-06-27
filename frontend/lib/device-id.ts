const DEVICE_ID_KEY = "beet_device_id"
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createDeviceId() {
    if (typeof crypto.randomUUID === "function") {
        return crypto.randomUUID()
    }

    const bytes = crypto.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0"))

    return [
        hex.slice(0, 4).join(""),
        hex.slice(4, 6).join(""),
        hex.slice(6, 8).join(""),
        hex.slice(8, 10).join(""),
        hex.slice(10, 16).join(""),
    ].join("-")
}

export function getOrCreateDeviceId() {
    const storedDeviceId = localStorage.getItem(DEVICE_ID_KEY)
    if (storedDeviceId && UUID_PATTERN.test(storedDeviceId)) {
        return storedDeviceId
    }

    const deviceId = createDeviceId()
    localStorage.setItem(DEVICE_ID_KEY, deviceId)
    return deviceId
}
