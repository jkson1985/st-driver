/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "Dawon Z-Wave Plug", namespace: "uqpqpu", author: "uqpqpu", ocfDeviceType: "oic.d.switch", runLocally: false, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false, genericHandler: "Z-Wave") { 
        capability "Energy Meter"
        capability "Actuator"
        capability "Switch"
        capability "Power Meter"
        capability "Refresh"
        capability "Configuration"
        capability "Sensor"
        capability "Light"
        capability "Health Check"

        command "reset"

        attribute "overCurrent", "string"
        attribute "hardware", "string"

        fingerprint inClusters: "0x25,0x32"
        fingerprint mfr: "018C", prod: "0042", model: "0005", deviceJoinName: "Dawon Smart Plug", ocfDeviceType: "oic.d.smartplug"
        fingerprint mfr: "018C", prod: "0042", model: "0008", deviceJoinName: "Dawon Smart Multitab", ocfDeviceType: "oic.d.smartplug"
    }

    // simulator metadata
    simulator {
        status "on":  "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"

        for (int i = 0; i <= 10000; i += 1000) {
            status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
                scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
        }
        for (int i = 0; i <= 100; i += 10) {
            status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
                scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
        }

        // reply messages
        reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
        reply "200100,delay 100,2502": "command: 2503, payload: 00"
    }


    preferences {
        input name: "stanbyPower", title:"Standby Power", type:"enum", options: ["Enable", "Disable"], description: "Stanby Power", required: true, defaultValue: "Disable"
        input name: "stanbyPowerValue", title:"Standby Power Value Setting" , type: "number", required: true, defaultValue: 00
        input name: "periodicMeasurement", type: "enum", title: "Periodic Measurement", options: ["Enable", "Disable"], description: "Periodic Measurement", required: true, defaultValue: "Enable"
        input name: "accumulation", type: "enum", title: "Accumulation", options: ["Stop", "Start"], description: "Accumulation", required: true, defaultValue: "Start"
        input name: "connectedDevice", type: "enum", title: "Connected Device", options: ["Use", "Not Use"], description: "Connected Device", required: true, defaultValue: "Use"
        input name: "minTimeInterval", type: "number", title: "Minimum time interval", description: "Minimum time interval", required: true, defaultValue: 1
    }

    // tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState("on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState:"turningOff")
                attributeState("off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn")
                attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff")
                attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn")
            }
        }
        valueTile("power", "device.power", width: 2, height: 2) {
            state "default", label:'${currentValue} W'
        }
        valueTile("energy", "device.energy", width: 2, height: 2) {
            state "default", label:'${currentValue} kWh'
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'reset kWh', action:"reset"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'', action:"configuration.configure", icon:"st.secondary.configure"
        }
        main(["switch","power","energy"])
        details(["switch","power","energy","refresh","reset","configure"])
    }
}

def installed() {
    log.debug "installed()"
    // Device-Watch simply pings if no device events received for 32min(checkInterval)
    initialize()
}

def updated() {
    try {
        if (!state.MSR) {
            response(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())
        }
    } catch (e) {
        log.debug e
    }

  initialize()
}

def initialize() {
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    configure()
}

def getCommandClassVersions() {
    [
        0x20: 1,  // Basic
        0x32: 3,  // Meter
        0x56: 1,  // Crc16Encap
        0x70: 1,  // Configuration
        0x72: 2,  // ManufacturerSpecific
        0x25: 1,  // Switch Binary
        0x27: 1,  // All Switch
        0x55: 1,  // Transport Service
        0x59: 1,  // AssociationGrpInfo
        0x5A: 1,  // DeviceResetLocally
        0x5E: 2,  // ZwaveplusInfo
        0x6C: 1,  // Supervision
        0x71: 3,    // Notification v8
        0x72: 2,  // ManufacturerSpecific
        0x73: 1,  // Powerlevel
        0x85: 2,  // Association
        0x86: 1,  // Version (2)
        0x8E: 2,  // Multi Channel Association
        0x98: 1,  // Security 0
        0x9F: 1   // Security S2
    ]
}

// parse events into attributes
def parse(String description) {
    log.debug "parse() - description: "+description
    def result = null
    if (description != "updated") {
        def cmd = zwave.parse(description, commandClassVersions)
        if (cmd) {
            result = zwaveEvent(cmd)
        } else {
            log.debug("Couldn't zwave.parse '$description'")
        }
    }
    result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {    
    if (cmd.meterType == 1) {
        if (cmd.scale == 0) {
            createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
        } else if (cmd.scale == 1) {
            createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
        } else if (cmd.scale == 2) {
            createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd){
    if(cmd.notificationType == 0x08){
        switch(cmd.event){
            case 0x02:
            sendEvent(name: "switch", value: "off")
            sendEvent(name: "overCurrent", value: "off")
            sendEvent(name: "hardware", value: "normal")
            break
            case 0x03:
            sendEvent(name: "switch", value: "on")
            sendEvent(name: "overCurrent", value: "off")
            sendEvent(name: "hardware", value: "normal")
            break
            case 0x06:
            sendEvent(name: "overCurrent", value: "on")
            sendEvent(name: "hardware", value: "normal")
            break
        }
    }else if(cmd.notificationType == 0x09){
        sendEvent(name: "overCurrent", value: "off")
        sendEvent(name: "hardware", value: "broken")
    }
    return []
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd){
    def value = (cmd.value ? "on" : "off")
    def evt = createEvent(name: "switch", value: value, type: "physical", descriptionText: "$device.displayName was turned $value")
    if (evt.isStateChange) {
        [evt, response(["delay 3000", meterGet(scale: 2).format()])]
    } else {
        evt
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd){
    switch(cmd.parameterNumber){
        case 1:
        def value = ( (cmd.configurationValue[0] << 16) + (cmd.configurationValue[1] << 8) + cmd.configurationValue[2] ) / 100        
        def enable = cmd.configurationValue[3] == 1 ? "Enable" : "Disable"
        log.info " - Standby Power: ${value}, Set: ${enable}"
        break
        case 2:
        log.info (cmd.configurationValue[0] == 1  ? " - Periodic Measurement Value Transmission Enable" : " - Periodic Measurement Value Transmission disable")
        break
        case 3:
        log.info (cmd.configurationValue[0] == 0  ? " - Accumulation Stop" : " - Accumulation Start")
        break
        case 4:
        log.info (cmd.configurationValue[0] == 0 ? " - Connected Device Not Use" : " - Connected Device Use")
        break
        case 5:
        log.info " - Time interval: " + (cmd.configurationValue[0] * 10) + "mins"
        break
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
    log.debug "Switch binary report: "+cmd
    def value = (cmd.value ? "on" : "off")
    createEvent(name: "switch", value: value, type: "digital", descriptionText: "$device.displayName was turned $value")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    def result = []

    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    log.debug "msr: $msr"
    updateDataValue("MSR", msr)

    result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "${device.displayName}: Unhandled: $cmd"
    return []
}

def on() {
    encapSequence([
        zwave.basicV1.basicSet(value: 0xFF),
        zwave.switchBinaryV1.switchBinaryGet(),
        meterGet(scale: 2)
    ], 3000)
}

def off() {
    encapSequence([
        zwave.basicV1.basicSet(value: 0x00),
        zwave.switchBinaryV1.switchBinaryGet(),
        meterGet(scale: 2)
    ], 3000)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    log.debug "ping()"
    refresh()
}

def poll() {
    sendHubCommand(refresh())
}

def refresh() {
    log.debug "refresh()"
    encapSequence([
        zwave.switchBinaryV1.switchBinaryGet(),
        meterGet(scale: 0),
        meterGet(scale: 2),
        zwave.configurationV1.configurationGet(parameterNumber: 1),
        zwave.configurationV1.configurationGet(parameterNumber: 2),
        zwave.configurationV1.configurationGet(parameterNumber: 3),
        zwave.configurationV1.configurationGet(parameterNumber: 4),
        zwave.configurationV1.configurationGet(parameterNumber: 5)
    ])
}

def configure() {
    log.info "configure()"
    def result = []

    log.debug "Configure zwaveInfo: "+zwaveInfo

    result << response(encap(meterGet(scale: 0)))
    result << response(encap(meterGet(scale: 2)))

    def _stanbyPower = stanbyPower
    if(_stanbyPower == null){
        _stanbyPower = "Disable"
    }
    def _stanbyPowerValue = stanbyPowerValue
    if(_stanbyPowerValue == null){
        _stanbyPowerValue = 0
    }
    def _periodicMeasurement = periodicMeasurement
    if(_periodicMeasurement == null){
        _periodicMeasurement = 1
    }
    def _accumulation = accumulation
    if(_accumulation == null){
        _accumulation = 1
    }
    def _connectedDevice = connectedDevice
    if(_connectedDevice == null){
        _connectedDevice = 1
    }
    def _minTimeInterval = minTimeInterval
    if(_minTimeInterval == null){
        _minTimeInterval = 1
    }

    def parameter1 = 0
    if (stanbyPower == "Enable") {
        parameter1 = (stanbyPowerValue << 8) + 1
    }

    log.debug "parameter1: ${parameter1}"


    result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 1, size: 4, scaledConfigurationValue: parameter1)))
    result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: (_periodicMeasurement == "Enable" ? 1 : 0) )))
    result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: (_accumulation == "Stop" ? 0 : 1) )))
    result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: (_connectedDevice == "Use" ? 1 : 0) )))
    result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 5, size: 1, scaledConfigurationValue: _minTimeInterval)))
    result
}

def reset() {
    encapSequence([
        meterReset(),
        meterGet(scale: 0)
    ])
}

def meterGet(map)
{
    return zwave.meterV2.meterGet(map)
}

def meterReset()
{
    return zwave.meterV2.meterReset()
}

/*
 * Security encapsulation support:
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
    if (encapsulatedCommand) {
        log.debug "Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}"
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract Secure command from $cmd"
    }
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def version = commandClassVersions[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (encapsulatedCommand) {
        log.debug "Parsed Crc16Encap into: ${encapsulatedCommand}"
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract CRC16 command from $cmd"
    }
}

private secEncap(physicalgraph.zwave.Command cmd) {
    log.debug "encapsulating command using Secure Encapsulation, command: $cmd"
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
    log.debug "encapsulating command using CRC16 Encapsulation, command: $cmd"
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private encap(physicalgraph.zwave.Command cmd) {
    if (zwaveInfo?.zw?.contains("s")) {
        secEncap(cmd)
    } else if (zwaveInfo?.cc?.contains("56")){
        crcEncap(cmd)
    } else {
        //    log.debug "no encapsulation supported for command: $cmd"
        cmd.format()
    }
}

private encapSequence(cmds, Integer delay=250) {
    delayBetween(cmds.collect{ encap(it) }, delay)
}