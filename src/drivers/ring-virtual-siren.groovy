/**
 *  Ring Virtual Siren Driver
 *
 *  Copyright 2019-2020 Ben Rimmasch
 *  Copyright 2021 Caleb Morse
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
  definition(name: "Ring Virtual Siren", namespace: "ring-hubitat-codahq", author: "Ben Rimmasch") {
    capability "Battery"
    capability "Refresh"
    capability "Sensor"
    capability "TamperAlert"
    //capability "Alarm" For now this is commented out because I can't see a way through the WS or API to turn the siren on
    //using the alarm hub's 'security-panel.sound-siren' set command does not work.  technically, the siren tests could be
    //chained back to back with a scheduled call back but leaving this as is for now

    attribute "commStatus", "enum", ["error", "ok", "update-queued", "updating", "waiting-for-join", "wrong-network"]
    attribute "firmware", "string"

    command "sirenTest"
    command "sirenTestCancel"
  }

  preferences {
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }
}

void logInfo(msg) {
  if (descriptionTextEnable) log.info msg
}

void logDebug(msg) {
  if (logEnable) log.debug msg
}

void logTrace(msg) {
  if (traceLogEnable) log.trace msg
}

void refresh() {
  parent.refresh(device.getDataValue("src"))
}

def sirenTest() {
  //TODO: make this impossible when alarm is armed. If you attempt this through Ring's UIs it is prevented
  //pearl is too deep a dive to add code so this device can ask the hub device what the mode is right now.
  parent.apiWebsocketRequestSetCommand("siren-test.start", device.getDataValue("src"), device.getDataValue("zid"))
}

def sirenTestCancel() {
  parent.apiWebsocketRequestSetCommand("siren-test.stop", device.getDataValue("src"), device.getDataValue("zid"))
}

void setValues(final Map deviceInfo) {
  logDebug "setValues(${deviceInfo})"

  if (deviceInfo.batteryLevel != null) {
    checkChanged("battery", deviceInfo.batteryLevel, "%")
  }

  // Update attributes where deviceInfo key is the same as attribute name and no conversion is necessary
  for (final entry in deviceInfo.subMap(["commStatus", "firmware", "tamper"])) {
    checkChanged(entry.key, entry.value)
  }

  // Update state values
  Map stateValues = deviceInfo.subMap(['impulseType', 'lastCommTime', 'lastUpdate', 'nextExpectedWakeup', 'signalStrength'])
  if (stateValues) {
	  state << stateValues
  }
}

void setPassthruValues(final Map deviceInfo) {
  logDebug "setPassthruValues(${deviceInfo})"

  if (deviceInfo.percent != null) {
    log.warn "${device.label} is updating firmware: ${deviceInfo.percent}% complete"
  }
}

void runCleanup() {
  device.removeDataValue('firmware') // Is an attribute now
}

boolean checkChanged(final String attribute, final newStatus, final String unit=null, final String type=null) {
  final boolean changed = device.currentValue(attribute) != newStatus
  if (changed) {
    logInfo "${attribute.capitalize()} for device ${device.label} is ${newStatus}"
  }
  sendEvent(name: attribute, value: newStatus, unit: unit, type: type)
  return changed
}