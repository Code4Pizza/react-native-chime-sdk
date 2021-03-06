//
//  MeetingModel.swift
//  AmazonChimeSDKDemo
//
//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: Apache-2.0
//

import AmazonChimeSDK
import AVFoundation
import UIKit

class MeetingModel: NSObject {
    enum ActiveMode {
        case roster
        case chat
        case video
        case screenShare
        case metrics
        case callKitOnHold
    }

    // Dependencies
    let meetingId: String
    let selfName: String
    let callKitOption: CallKitOption
    let meetingSessionConfig: MeetingSessionConfiguration
    lazy var currentMeetingSession = DefaultMeetingSession(configuration: meetingSessionConfig,
                                                           logger: logger)

    // Utils
    let logger = ConsoleLogger(name: "MeetingModel", level: .OFF)
    let postLogger: PostLogger
    let activeSpeakerObserverId = UUID().uuidString

    // Sub models
    let rosterModel = RosterModel()
    lazy var videoModel = VideoModel(audioVideoFacade: currentMeetingSession.audioVideo,
                                     eventAnalyticsController: currentMeetingSession.eventAnalyticsController)
    let metricsModel = MetricsModel()
    lazy var screenShareModel = ScreenShareModel(meetingSessionConfig: meetingSessionConfig,
                                                 contentShareController: currentMeetingSession.audioVideo)
    let chatModel = ChatModel()
    lazy var deviceSelectionModel = DeviceSelectionModel(deviceController: currentMeetingSession.audioVideo,
                                                         cameraCaptureSource: videoModel.customSource)
    let uuid = UUID()
    var call: Call?
    var mapUserAudioState = [String:Int]()
    
    // States
    var isAppInBackground: Bool = false {
        didSet {
            if isAppInBackground {
                wasLocalVideoOn = videoModel.isLocalVideoActive
                if wasLocalVideoOn {
                    videoModel.isLocalVideoActive = false
                }
                videoModel.pauseAllRemoteVideos()
            } else {
                if wasLocalVideoOn {
                    videoModel.isLocalVideoActive = true
                }
                videoModel.resumeAllRemoteVideosInCurrentPageExceptUserPausedVideos()
            }
        }
    }
    private var savedModeBeforeOnHold: ActiveMode?
    private var wasLocalVideoOn: Bool = false

    var activeMode: ActiveMode = .roster {
        didSet {
            if activeMode == .video {
                videoModel.resumeAllRemoteVideosInCurrentPageExceptUserPausedVideos()
            } else {
                videoModel.pauseAllRemoteVideos()
            }
            activeModeDidSetHandler?(activeMode)
        }
    }

    private var isMuted = false {
        didSet {
            if isMuted {
                if currentMeetingSession.audioVideo.realtimeLocalMute() {
                    logger.info(msg: "Microphone has been muted")
                }
            } else {
                if currentMeetingSession.audioVideo.realtimeLocalUnmute() {
                    logger.info(msg: "Microphone has been unmuted")
                }
            }
            isMutedHandler?(isMuted)
        }
    }

    private var isEnded = false {
        didSet {
            // This will unbind current tiles.
            videoModel.isEnded = true
            currentMeetingSession.audioVideo.stop()
            screenShareModel.stopLocalSharing()
            isEndedHandler?()
        }
    }

    var audioDevices: [MediaDevice] {
        return currentMeetingSession.audioVideo.listAudioDevices()
    }

    var currentAudioDevice: MediaDevice? {
        return currentMeetingSession.audioVideo.getActiveAudioDevice()
    }

    // Handlers
    var activeModeDidSetHandler: ((ActiveMode) -> Void)?
    var notifyHandler: ((String) -> Void)?
    var isMutedHandler: ((Bool) -> Void)?
    var isEndedHandler: (() -> Void)?

    init(meetingSessionConfig: MeetingSessionConfiguration,
         meetingId: String,
         selfName: String,
         callKitOption: CallKitOption) {

        self.meetingId = meetingId
        self.selfName = selfName
        self.callKitOption = callKitOption
        self.meetingSessionConfig = meetingSessionConfig
        let url = AppConfiguration.url.hasSuffix("/") ? AppConfiguration.url : "\(AppConfiguration.url)/"
        self.postLogger = PostLogger(name: "SDKEvents", configuration: meetingSessionConfig, url: "\(url)log_meeting_event")
        super.init()

        if callKitOption == .incoming {
            call = createCall(isOutgoing: false)
        } else if callKitOption == .outgoing {
            call = createCall(isOutgoing: true)
        }
    }

    func bind(videoRenderView: VideoRenderView, tileId: Int) {
        currentMeetingSession.audioVideo.bindVideoView(videoView: videoRenderView, tileId: tileId)
    }

    func startMeeting() {
        if self.callKitOption == .disabled {
            self.configureAudioSession()
            self.startAudioVideoConnection(isCallKitEnabled: false)
            self.currentMeetingSession.audioVideo.startRemoteVideo()
        }
        screenShareModel.broadcastCaptureModel.isBlocked = false
    }

    func resumeCallKitMeeting() {
        if let call = call {
            CallKitManager.shared().setHeld(with: call, isOnHold: false)
        }
    }

    func endMeeting() {
        if let call = call, !isEnded {
            CallKitManager.shared().endCallFromLocal(with: call)
        } else {
            isEnded = true
        }
    }

    func setMute(isMuted: Bool) {
        if let call = call {
            CallKitManager.shared().setMuted(for: call, isMuted: isMuted)
        } else {
            self.isMuted = isMuted
        }
    }

    func setVoiceFocusEnabled(enabled: Bool) {
        let action = enabled ? "enable" : "disable"
        let success = currentMeetingSession.audioVideo.realtimeSetVoiceFocusEnabled(enabled: enabled)
        if success {
            notify(msg: "Voice Focus \(action)d")
        } else {
            notify(msg: "Failed to \(action) Voice Focus")
        }
    }

    func isVoiceFocusEnabled() -> Bool {
        return currentMeetingSession.audioVideo.realtimeIsVoiceFocusEnabled()
    }

    func getVideoTileDisplayName(for indexPath: IndexPath) -> String {
        var displayName = ""
        if indexPath.item == 0 {
            if videoModel.isLocalVideoActive {
                displayName = selfName
            } else {
                displayName = "Turn on your video"
            }
        } else {
            if let videoTileState = videoModel.getVideoTileState(for: indexPath) {
                displayName = rosterModel.getAttendeeName(for: videoTileState.attendeeId) ?? ""
            }
        }
        return displayName
    }

    func chooseAudioDevice(_ audioDevice: MediaDevice) {
        currentMeetingSession.audioVideo.chooseAudioDevice(mediaDevice: audioDevice)
        MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
            "event": "audio_device_changed",
            "mediaDeviceLabel": audioDevice.label,
            "mediaDeviceType": audioDevice.type.rawValue
        ])
    }
    
    func selectAudioDevice(_ mediaDevice: [String:Any]) {
        if audioDevices.count > 0 {
            let label = mediaDevice["label"] as! String
            let type = mediaDevice["type"] as! Int
            
            var audioDevice:MediaDevice? = nil
            for item in audioDevices {
                if item.type.rawValue == type && item.label == label {
                    audioDevice = item
                }
            }
            if audioDevice != nil {
                self.chooseAudioDevice(audioDevice!)
            }
        }
    }
    
    func selectVideoDevice(_ mediaDevice: [String:Any]) {
        let label = mediaDevice["label"] as! String
        let type = mediaDevice["type"] as! Int
        
        var videoDevice:MediaDevice? = nil
        for item in MediaDevice.listVideoDevices() {
            if item.type.rawValue == type && item.label == label {
                videoDevice = item
            }
        }
        if videoDevice != nil {
            self.videoModel.customSource.device = videoDevice
            MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                "event": "video_device_Changed",
                "mediaDeviceLabel": videoDevice!.label,
                "mediaDeviceType": videoDevice!.type.rawValue
            ])
        }
    }

    func sendDataMessage(_ message: String) {
        do {
            try currentMeetingSession
                .audioVideo
                .realtimeSendDataMessage(topic: "chat",
                                         data: message,
                                         lifetimeMs: 1000)
        } catch {
            logger.error(msg: "Failed to send message!")
            return
        }

        let currentTimestamp = NSDate().timeIntervalSince1970
        let timestamp = TimeStampConversion.formatTimestamp(timestamp: Int64(currentTimestamp) * 1000)

        chatModel
            .addChatMessage(chatMessage:
                ChatMessage(
                    senderName: self.selfName,
                    message: message,
                    timestamp: timestamp,
                    isSelf: true
                ))
    }

    private func notify(msg: String) {
        logger.info(msg: msg)
        notifyHandler?(msg)
    }

    private func logWithFunctionName(fnName: String = #function, message: String = "") {
        logger.info(msg: "[Function] \(fnName) -> \(message)")
    }

    private func setupAudioVideoFacadeObservers() {
        let audioVideo = currentMeetingSession.audioVideo
        audioVideo.addVideoTileObserver(observer: self)
        audioVideo.addRealtimeObserver(observer: self)
        audioVideo.addAudioVideoObserver(observer: self)
        audioVideo.addMetricsObserver(observer: self)
        audioVideo.addDeviceChangeObserver(observer: self)
        audioVideo.addActiveSpeakerObserver(policy: DefaultActiveSpeakerPolicy(),
                                            observer: self)
        audioVideo.addRealtimeDataMessageObserver(topic: "chat", observer: self)
        audioVideo.addEventAnalyticsObserver(observer: self)
    }

    private func removeAudioVideoFacadeObservers() {
        let audioVideo = currentMeetingSession.audioVideo
        audioVideo.removeVideoTileObserver(observer: self)
        audioVideo.removeRealtimeObserver(observer: self)
        audioVideo.removeAudioVideoObserver(observer: self)
        audioVideo.removeMetricsObserver(observer: self)
        audioVideo.removeDeviceChangeObserver(observer: self)
        audioVideo.removeActiveSpeakerObserver(observer: self)
        audioVideo.removeRealtimeDataMessageObserverFromTopic(topic: "chat")
        audioVideo.removeEventAnalyticsObserver(observer: self)
    }

    private func configureAudioSession() {
        MeetingModule.shared().configureAudioSession()
    }

    private func startAudioVideoConnection(isCallKitEnabled: Bool) {
        do {
            setupAudioVideoFacadeObservers()
            try currentMeetingSession.audioVideo.start(callKitEnabled: isCallKitEnabled)
        } catch {
            logger.error(msg: "Error starting the Meeting: \(error.localizedDescription)")
            endMeeting()
        }
    }

    private func logAttendee(attendeeInfo: [AttendeeInfo], action: String) {
        for currentAttendeeInfo in attendeeInfo {
            let attendeeId = currentAttendeeInfo.attendeeId
            if !rosterModel.contains(attendeeId: attendeeId) {
                logger.error(msg: "Cannot find attendee with attendee id \(attendeeId)" +
                    " external user id \(currentAttendeeInfo.externalUserId): \(action)")
                continue
            }
            logger.info(msg: "\(rosterModel.getAttendeeName(for: attendeeId) ?? "nil"): \(action)")
        }
    }

    private func createCall(isOutgoing: Bool) -> Call {
        let call = Call(uuid: uuid, handle: meetingId, isOutgoing: isOutgoing)
        call.isReadytoConfigureHandler = { [weak self] in
            self?.configureAudioSession()
        }
        call.isAudioSessionActiveHandler = { [weak self] in
            self?.startAudioVideoConnection(isCallKitEnabled: true)
            self?.currentMeetingSession.audioVideo.startRemoteVideo()
            if self?.isMuted ?? false {
                _ = self?.currentMeetingSession.audioVideo.realtimeLocalMute()
            }
        }
        call.isEndedHandler = { [weak self] in
            self?.isEnded = true
        }
        call.isMutedHandler = { [weak self] isMuted in
            self?.isMuted = isMuted
        }
        call.isOnHoldHandler = { [weak self] isOnHold in
            if isOnHold {
                self?.currentMeetingSession.audioVideo.stop()
                self?.savedModeBeforeOnHold = self?.activeMode
                self?.activeMode = .callKitOnHold
            } else {
                if let savedModeBeforeOnHold = self?.savedModeBeforeOnHold {
                    self?.activeMode = savedModeBeforeOnHold
                    self?.savedModeBeforeOnHold = nil
                }
            }
        }
        return call
    }
}

// MARK: AudioVideoObserver

extension MeetingModel: AudioVideoObserver {
    func connectionDidRecover() {
        notifyHandler?("Connection quality has recovered")
        logWithFunctionName()
    }

    func connectionDidBecomePoor() {
        print("+++ connectionDidBecomePoor")
        notifyHandler?("Connection quality has become poor")
        logWithFunctionName()
    }

    func videoSessionDidStopWithStatus(sessionStatus: MeetingSessionStatus) {
        logWithFunctionName(message: "\(sessionStatus.statusCode)")
    }

    func audioSessionDidStartConnecting(reconnecting: Bool) {
        notifyHandler?("Audio started connecting. Reconnecting: \(reconnecting)")
        logWithFunctionName(message: "reconnecting \(reconnecting)")
        if !reconnecting {
            call?.isConnectingHandler?()
        }
    }

    func audioSessionDidStart(reconnecting: Bool) {
        notifyHandler?("Audio successfully started. Reconnecting: \(reconnecting)")
        logWithFunctionName(message: "reconnecting \(reconnecting)")
        // Start Amazon Voice Focus as soon as audio session started
        setVoiceFocusEnabled(enabled: false)
        if !reconnecting {
            call?.isConnectedHandler?()
        }

        // This selection has to be here because if there are bluetooth headset connected,
        // selecting non-bluetooth device before audioVideo.start() will get route overwritten by bluetooth
        // after audio session starts
        chooseAudioDevice(deviceSelectionModel.selectedAudioDevice)
    }

    func audioSessionDidDrop() {
        notifyHandler?("Audio Session Dropped")
        logWithFunctionName()
    }

    func audioSessionDidStopWithStatus(sessionStatus: MeetingSessionStatus) {
        logWithFunctionName(message: "\(sessionStatus.statusCode)")

        removeAudioVideoFacadeObservers()
        if let call = call {
            switch sessionStatus.statusCode {
            case .ok:
                if call.isOnHold {
                    return
                }
            case .audioCallEnded, .audioServerHungup:
                CallKitManager.shared().reportCallEndedFromRemote(with: call, reason: .remoteEnded)
            case .audioJoinedFromAnotherDevice:
                CallKitManager.shared().reportCallEndedFromRemote(with: call, reason: .answeredElsewhere)
            case .audioDisconnectAudio:
                CallKitManager.shared().reportCallEndedFromRemote(with: call, reason: .declinedElsewhere)
            default:
                CallKitManager.shared().reportCallEndedFromRemote(with: call, reason: .failed)
            }
        }
        endMeeting()
    }

    func audioSessionDidCancelReconnect() {
        notifyHandler?("Audio cancelled reconnecting")
        logWithFunctionName()
    }

    func videoSessionDidStartConnecting() {
        logWithFunctionName()
    }

    func videoSessionDidStartWithStatus(sessionStatus: MeetingSessionStatus) {
        switch sessionStatus.statusCode {
        case .videoAtCapacityViewOnly:
            notifyHandler?("Maximum concurrent video limit reached! Failed to start local video")
            logWithFunctionName(message: "\(sessionStatus.statusCode)")
        default:
            logWithFunctionName(message: "\(sessionStatus.statusCode)")
        }
    }
}

// MARK: RealtimeObserver

extension MeetingModel: RealtimeObserver {
    private func removeAttendeesAndReload(attendeeInfo: [AttendeeInfo]) {
        let attendeeIds = attendeeInfo.map { $0.attendeeId }
        rosterModel.removeAttendees(attendeeIds)
        if activeMode == .roster {
            rosterModel.rosterUpdatedHandler?()
        }
    }

    func attendeesDidLeave(attendeeInfo: [AttendeeInfo]) {
        logAttendee(attendeeInfo: attendeeInfo, action: "Left")
        removeAttendeesAndReload(attendeeInfo: attendeeInfo)
        for currentAttendeeInfo in attendeeInfo {
            let attendeeId = currentAttendeeInfo.attendeeId
            videoModel.removeAllTileStateForId(attendeeId)
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : attendeeId])
                
                let attendeeName = RosterModel.convertAttendeeName(from: currentAttendeeInfo)
                MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                    "event": "sinkMeetingUserLeft",
                    "userID": attendeeId,
                    "userName": attendeeName,
                    "audioStatus": self.getAudioStatus(attendeeId),
                    "videoStatus": self.getVideoStatus(attendeeId),
                ])
            }
        }
    }
    func getVideoStatus(_ attendeeId: String) -> Int {
        if let _ = videoModel.getRemoteVideoTileState(attendeeId) {
            return 1
        }
        return 0
    }
    func getAudioStatus(_ attendeeId: String) -> Int {
        let audio = self.mapUserAudioState[attendeeId] ?? 0
        return audio
    }
    func attendeesDidDrop(attendeeInfo: [AttendeeInfo]) {
        for attendee in attendeeInfo {
            notify(msg: "\(attendee.externalUserId) dropped")
        }

        removeAttendeesAndReload(attendeeInfo: attendeeInfo)
    }

    func attendeesDidMute(attendeeInfo: [AttendeeInfo]) {
        logAttendee(attendeeInfo: attendeeInfo, action: "Muted")
        for attendee in attendeeInfo {
            let attendeeId = attendee.attendeeId
            let oldValue = self.mapUserAudioState[attendeeId] ?? -2
            let newValue = 0
            if oldValue != newValue {
                self.mapUserAudioState[attendeeId] = newValue != -1 ? 1 : 0
                self.sendAudioEvent(attendeeId)
            }
        }
    }

    func attendeesDidUnmute(attendeeInfo: [AttendeeInfo]) {
        logAttendee(attendeeInfo: attendeeInfo, action: "Unmuted")
        for attendee in attendeeInfo {
            let attendeeId = attendee.attendeeId
            let oldValue = self.mapUserAudioState[attendeeId] ?? -2
            let newValue = 1
            if oldValue != newValue {
                self.mapUserAudioState[attendeeId] = newValue != -1 ? 1 : 0
                self.sendAudioEvent(attendeeId)
            }
        }
    }
    func sendAudioEvent(_ attendeeId:String) {
        if let attendee = self.rosterModel.getAttendee(attendeeId: attendeeId) {
            MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                "event": "sinkMeetingAudioStatusChange",
                "userID": attendeeId,
                "userName": attendee.attendeeName ?? "",
                "audioStatus": self.getAudioStatus(attendeeId),
                "videoStatus": self.getVideoStatus(attendeeId),
            ])
        }
    }
    func volumeDidChange(volumeUpdates: [VolumeUpdate]) {
        for currentVolumeUpdate in volumeUpdates {
            let attendeeId = currentVolumeUpdate.attendeeInfo.attendeeId
            let oldValue = self.mapUserAudioState[attendeeId] ?? -2
            let newValue = currentVolumeUpdate.volumeLevel.rawValue != -1 ? 1 : 0
            if oldValue != newValue {
                self.mapUserAudioState[attendeeId] = newValue
                self.sendAudioEvent(attendeeId)
            }
            rosterModel.updateVolume(attendeeId: attendeeId, volume: currentVolumeUpdate.volumeLevel)
        }
        if activeMode == .roster {
            rosterModel.rosterUpdatedHandler?()
        }
    }

    func signalStrengthDidChange(signalUpdates: [SignalUpdate]) {
        for currentSignalUpdate in signalUpdates {
            logWithFunctionName(message: "\(currentSignalUpdate.attendeeInfo.externalUserId) \(currentSignalUpdate.signalStrength)")
            let attendeeId = currentSignalUpdate.attendeeInfo.attendeeId
            rosterModel.updateSignal(attendeeId: attendeeId, signal: currentSignalUpdate.signalStrength)
        }
        if activeMode == .roster {
            rosterModel.rosterUpdatedHandler?()
        }
    }

    func attendeesDidJoin(attendeeInfo: [AttendeeInfo]) {
        var newAttendees = [RosterAttendee]()
        for currentAttendeeInfo in attendeeInfo {
            let attendeeId = currentAttendeeInfo.attendeeId
            if !rosterModel.contains(attendeeId: attendeeId) {
                let attendeeName = RosterModel.convertAttendeeName(from: currentAttendeeInfo)
                MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                    "event": "sinkMeetingUserJoin",
                    "userID": attendeeId,
                    "userName": attendeeName,
                    "audioStatus": self.getAudioStatus(attendeeId),
                    "videoStatus": self.getVideoStatus(attendeeId),
                ])
                if attendeeName == self.selfName {
                    MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: ["des": "meeting_ready"])
                }
                let newAttendee = RosterAttendee(attendeeId: attendeeId,
                                                 attendeeName: attendeeName,
                                                 volume: .notSpeaking,
                                                 signal: .high)
                newAttendees.append(newAttendee)

                // if other attendee starts sharing content, stop content sharing from current device
                let modality = DefaultModality(id: attendeeId)
                if modality.isOfType(type: .content),
                   modality.base != meetingSessionConfig.credentials.attendeeId {

                    screenShareModel.stopLocalSharing()
                    notifyHandler?("\(rosterModel.getAttendeeName(for: modality.base) ?? "") took over the screen share")
                }
            }
        }
        rosterModel.addAttendees(newAttendees)
        if activeMode == .roster {
            rosterModel.rosterUpdatedHandler?()
        }
    }
}

// MARK: MetricsObserver

extension MeetingModel: MetricsObserver {
    func metricsDidReceive(metrics: [AnyHashable: Any]) {
        guard let observableMetrics = metrics as? [ObservableMetric: Any] else {
            logger.error(msg: "The received metrics \(metrics) is not of type [ObservableMetric: Any].")
            return
        }
        metricsModel.updateAppMetrics(metrics: metrics)
        logger.info(msg: "Media metrics have been received: \(observableMetrics)")
        if activeMode == .metrics {
            metricsModel.metricsUpdatedHandler?()
        }
    }
}

// MARK: DeviceChangeObserver

extension MeetingModel: DeviceChangeObserver {
    func audioDeviceDidChange(freshAudioDeviceList: [MediaDevice]) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.selectNewAudioDevices()
        }
        
        let deviceLabels: [String] = freshAudioDeviceList.map { device in "* \(device.label) (\(device.type))" }
        logger.info(msg: deviceLabels.joined(separator: "\n"))
        notifyHandler?("Device availability changed:\nAvailable Devices:\n\(deviceLabels.joined(separator: "\n"))")
    }
    func selectNewAudioDevices() {
        if audioDevices.count > 0 {
            var audioDevice = audioDevices[0];
            for item in audioDevices {
                if audioDevice.type.rawValue > item.type.rawValue {
                    audioDevice = item
                }
            }
            self.chooseAudioDevice(audioDevice)
        }
    }
}

// MARK: VideoTileObserver

extension MeetingModel: VideoTileObserver {
    func videoTileDidAdd(tileState: VideoTileState) {
        print("+++ videoTileDidAdd")
        logger.error(msg: "Attempting to add video tile tileId: \(tileState.tileId)" +
            " attendeeId: \(tileState.attendeeId) with size \(tileState.videoStreamContentWidth)*\(tileState.videoStreamContentHeight)")
                       
        if tileState.isContent {
            screenShareModel.tileId = tileState.tileId
            if activeMode == .screenShare {
                screenShareModel.viewUpdateHandler?(true)
            }
            
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: Notification.Name("onShareVideoStatusChangedChime"), object: nil, userInfo: nil)
                
                if let attendee = self.rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
                    MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                        "event": "sinkMeetingActiveShare",
                        "shareStatus": 1,
                        "userID": attendee.attendeeId
                    ])
                }
            }
            
        } else {
            videoModel.removeAllTileStateForId(tileState.attendeeId)
            if tileState.isLocalTile {
                videoModel.setSelfVideoTileState(tileState)
                if activeMode == .video {
                    videoModel.localVideoUpdatedHandler?()
                }
                videoModel.addRemoteVideoTileState(tileState, completion: { success in
                    if success {
                        if self.activeMode == .video {
                            self.pauseRemoteVideo(videoTileState: tileState)
                            self.videoModel.videoUpdatedHandler?()
                        } else {
                            // Currently not in the video view, no need to render the video tile
                            self.pauseRemoteVideo(videoTileState: tileState)
                        }
                    } else {
                        self.logger.info(msg: "Cannot add more video tile tileId: \(tileState.tileId)")
                    }
                })
            } else {
                videoModel.addRemoteVideoTileState(tileState, completion: { success in
                    if success {
                        if self.activeMode == .video {
                            // If the video is not currently being displayed, pause it
//                            if !self.videoModel.isRemoteVideoDisplaying(tileId: tileState.tileId) {
//                                self.currentMeetingSession.audioVideo.pauseRemoteVideoTile(tileId: tileState.tileId)
//                            }
                            self.pauseRemoteVideo(videoTileState: tileState)
                            self.videoModel.videoUpdatedHandler?()
                        } else {
                            // Currently not in the video view, no need to render the video tile
                            self.pauseRemoteVideo(videoTileState: tileState)
                        }
                    } else {
                        self.logger.info(msg: "Cannot add more video tile tileId: \(tileState.tileId)")
                    }
                })
            }
                        
            DispatchQueue.main.async {
                if let attendee = self.rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
                    MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                        "event": "sinkMeetingVideoStatusChange",
                        "userID": tileState.attendeeId,
                        "userName": attendee.attendeeName ?? "",
                        "audioStatus": self.getAudioStatus(tileState.attendeeId),
                        "videoStatus": self.getVideoStatus(tileState.attendeeId),
                    ])
                }
                
                NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : tileState.attendeeId])
            }
        }
    }
func pauseRemoteVideo(videoTileState: VideoTileState) {
    let queue = MeetingModule.shared().getTileQueue(videoTileState.tileId)
    let operationId = UUID().uuidString
    let operation = SynchronousOperation { _ in
        if (queue.lastOperationId == operationId) {
            Thread.sleep(forTimeInterval: 1.5)
            if (queue.lastOperationId == operationId) {
                print("+++ thuc thi pause")
                MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.pauseRemoteVideoTile(tileId: videoTileState.tileId)
            }
            else {
                //print("+++ bo qua pause")
            }
        }
        else {
            //print("+++ bo qua pause")
        }
    }
    operation.name = operationId
    queue.addOperation(operation)
}
    func videoTileDidRemove(tileState: VideoTileState) {
        logger.error(msg: "Attempting to remove video tile tileId: \(tileState.tileId)" +
            " attendeeId: \(tileState.attendeeId)")
        currentMeetingSession.audioVideo.unbindVideoView(tileId: tileState.tileId)

        if tileState.isContent {
            screenShareModel.tileId = nil
            if activeMode == .screenShare {
                screenShareModel.viewUpdateHandler?(false)
            }
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: Notification.Name("onShareVideoStatusChangedChime"), object: nil, userInfo: nil)
                
                MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                    "event": "sinkMeetingActiveShare",
                    "shareStatus": 4
                ])
            }
        } else if tileState.isLocalTile {
            videoModel.removeAllTileStateForId(tileState.attendeeId)
            videoModel.setSelfVideoTileState(nil)
            if activeMode == .video {
                videoModel.localVideoUpdatedHandler?()
            }
            videoModel.removeRemoteVideoTileState(tileState, completion: { success in
                if success {
                    self.videoModel.revalidateRemoteVideoPageIndex()
                    if self.activeMode == .video {
                        self.videoModel.videoUpdatedHandler?()
                    }
                } else {
                    self.logger.error(msg: "Cannot remove unexisting remote video tile for tileId: \(tileState.tileId)")
                }
            })
            
            DispatchQueue.main.async {
                if let attendee = self.rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
                    MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                        "event": "sinkMeetingVideoStatusChange",
                        "userID": tileState.attendeeId,
                        "userName": attendee.attendeeName ?? "",
                        "audioStatus": self.getAudioStatus(tileState.attendeeId),
                        "videoStatus": self.getVideoStatus(tileState.attendeeId),
                    ])
                }
                
                NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : tileState.attendeeId])
            }
            
        } else {
            videoModel.removeAllTileStateForId(tileState.attendeeId)
            videoModel.removeRemoteVideoTileState(tileState, completion: { success in
                if success {
                    self.videoModel.revalidateRemoteVideoPageIndex()
                    if self.activeMode == .video {
                        self.videoModel.videoUpdatedHandler?()
                    }
                } else {
                    self.logger.error(msg: "Cannot remove unexisting remote video tile for tileId: \(tileState.tileId)")
                }
            })
            
            DispatchQueue.main.async {
                if let attendee = self.rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
                    MeetingModule.shared().eventEmitter?.sendEvent(withName: "onChimeMeetingEvent", body: [
                        "event": "sinkMeetingVideoStatusChange",
                        "userID": tileState.attendeeId,
                        "userName": attendee.attendeeName ?? "",
                        "audioStatus": self.getAudioStatus(tileState.attendeeId),
                        "videoStatus": self.getVideoStatus(tileState.attendeeId),
                    ])
                }
                
                NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : tileState.attendeeId])
            }
        }
    }

    func videoTileDidPause(tileState: VideoTileState) {
        if let attendee = rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
            print("+++ ok videoTileDidPause \(attendee.attendeeName) - poor: \(tileState.pauseState == .pausedForPoorConnection)")
        }
        if tileState.pauseState == .pausedForPoorConnection {
            if let attendee = rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
                print("+++ ok pausedForPoorConnection \(attendee.attendeeName)")
//                DispatchQueue.main.async {
//                NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : tileState.attendeeId])
//                }
            }
        } else {
            let attendeeId = tileState.attendeeId
            let attendeeName = rosterModel.getAttendeeName(for: attendeeId) ?? ""
            notifyHandler?("Video for attendee \(attendeeName) " +
                " has been paused")
        }
    }

    func videoTileDidResume(tileState: VideoTileState) {
        if let attendee = rosterModel.getAttendee(attendeeId: tileState.attendeeId) {
            print("+++ ok videoTileDidResume \(attendee.attendeeName)")
        }
        let attendeeId = tileState.attendeeId
        let attendeeName = rosterModel.getAttendeeName(for: attendeeId) ?? ""
        notifyHandler?("Video for attendee \(attendeeName) has been unpaused")
        videoModel.updateRemoteVideoTileState(tileState)
//        DispatchQueue.main.async {
//        NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : tileState.attendeeId])
//        }
    }

    func videoTileSizeDidChange(tileState: VideoTileState) {
        logger.info(msg: "Video stream content size changed to \(tileState.videoStreamContentWidth)*\(tileState.videoStreamContentHeight) for tileId: \(tileState.tileId)")
    }
}

// MARK: ActiveSpeakerObserver

extension MeetingModel: ActiveSpeakerObserver {
    var observerId: String {
        return activeSpeakerObserverId
    }

    var scoresCallbackIntervalMs: Int {
        return 5000 // 5 second
    }

    func activeSpeakerDidDetect(attendeeInfo: [AttendeeInfo]) {
        videoModel.updateRemoteVideoStatesBasedOnActiveSpeakers(activeSpeakers: attendeeInfo)
        if activeMode == .video {
            videoModel.videoUpdatedHandler?()
        }

        rosterModel.updateActiveSpeakers(attendeeInfo.map { $0.attendeeId })
        if activeMode == .roster {
            rosterModel.rosterUpdatedHandler?()
        }
    }

    func activeSpeakerScoreDidChange(scores: [AttendeeInfo: Double]) {
        let scoresInString = scores.map { (score) -> String in
            let (key, value) = score
            return "\(key.externalUserId): \(value)"
        }.joined(separator: ",")
        logWithFunctionName(message: "\(scoresInString)")
    }
}

// MARK: DataMessageObserver

extension MeetingModel: DataMessageObserver {
    func dataMessageDidReceived(dataMessage: DataMessage) {
        chatModel.addDataMessage(dataMessage: dataMessage)
    }
}

extension MeetingModel: EventAnalyticsObserver {
    func eventDidReceive(name: EventName, attributes: [AnyHashable: Any]) {
        let jsonData = try? JSONSerialization.data(withJSONObject: [
            "name": "\(name)",
            "attributes": toStringKeyDict(attributes.merging(currentMeetingSession.audioVideo.getCommonEventAttributes(),
                                                             uniquingKeysWith: { (_, newVal) -> Any in
                newVal
            }))
        ], options: [])

        guard let data = jsonData, let msg = String(data: data, encoding: .utf8)  else {
            logger.info(msg: "Dictionary is not in correct format to be serialized")
            return
        }
        postLogger.info(msg: msg)

        switch name {
        case .meetingStartSucceeded:
            logger.info(msg: "Meeting stared on : \(currentMeetingSession.audioVideo.getCommonEventAttributes().toJsonString())")
        case .meetingEnded, .meetingFailed:
            logger.info(msg: "\(currentMeetingSession.audioVideo.getMeetingHistory())")
            postLogger.publishLog()
        default:
            break
        }
    }

    func toStringKeyDict(_ attributes: [AnyHashable: Any]) -> [String: Any] {
        var jsonDict = [String: Any]()
        attributes.forEach { (key, value) in
            jsonDict[String(describing: key)] = String(describing: value)
        }
        return jsonDict
    }
}
