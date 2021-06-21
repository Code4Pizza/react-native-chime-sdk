//
//  MeetingModule.swift
//  AmazonChimeSDKDemo
//
//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: Apache-2.0
//

import AmazonChimeSDK
import AVFoundation
import UIKit

let incomingCallKitDelayInSeconds = 10.0

@objc(MeetingModule)
class MeetingModule: NSObject {
    private static var sharedInstance: MeetingModule?
    private(set) var activeMeeting: MeetingModel?
    private let meetingPresenter = MeetingPresenter()
    private var meetings: [UUID: MeetingModel] = [:]
    private let logger = ConsoleLogger(name: "MeetingModule")
    public weak var eventEmitter: RCTEventEmitter?
    
    @objc(shared)
    static func shared() -> MeetingModule {
        if sharedInstance == nil {
            sharedInstance = MeetingModule()

            // This is to initialize CallKit properly before requesting first outgoing/incoming call
            _ = CallKitManager.shared()
        }
        return sharedInstance!
    }
    
    func prepareMeeting(meetingId: String,
                        selfName: String,
                        option: CallKitOption,
                        overriddenEndpoint: String,
                        completion: @escaping (Bool) -> Void) {
        requestRecordPermission { success in
            guard success else {
                completion(false)
                return
            }
            JoinRequestService.postJoinRequest(meetingId: meetingId, name: selfName, overriddenEndpoint: overriddenEndpoint) { (json) in
                self.prepareMeetingWithJson(json: json, completion: completion)
            }
        }
    }
    
    func getJsonMeeting(meetingId: String,
                        selfName: String,
                        option: CallKitOption,
                        overriddenEndpoint: String,
                        completion: @escaping ([String:Any]?) -> Void) {
        requestRecordPermission { success in
            guard success else {
                completion(nil)
                return
            }
            JoinRequestService.postJoinRequest(meetingId: meetingId, name: selfName, overriddenEndpoint: overriddenEndpoint) { (json) in
                completion(json)
            }
        }
    }
    @objc(setRCTEventEmitter:)
    func setRCTEventEmitter(eventEmitter: RCTEventEmitter?) {
        self.eventEmitter = eventEmitter
    }
    @objc(onMyAudio)
    func onMyAudio() {
        activeMeeting?.setMute(isMuted: false)
    }
    
    @objc(offMyAudio)
    func offMyAudio() {
        activeMeeting?.setMute(isMuted: true)
    }
    
    @objc(onOffMyVideo)
    func onOffMyVideo() {
        if let activeMeeting = self.activeMeeting {
            activeMeeting.videoModel.isLocalVideoActive = !activeMeeting.videoModel.isLocalVideoActive
        }
    }
    
    @objc(getParticipants:)
    func getParticipants(completion: ([[String:Any]]) -> Void) {
        if let activeMeeting = self.activeMeeting {
            var list = [[String:Any]]()
            for i in 0..<activeMeeting.rosterModel.getTotalAttendee() {
                if let member = activeMeeting.rosterModel.getAttendee(at: i) {
                    list.append([
                        "userName": member.attendeeName ?? "",
                        "userID": member.attendeeId,
                        "audioStatus": activeMeeting.getAudioStatus(member.attendeeId),
                        "videoStatus": activeMeeting.getVideoStatus(member.attendeeId),
                    ]);
                }
            }
            completion(list)
        }
    }
    
    @objc(getUserInfo:completion:)
    func getUserInfo(userId:String, completion: ([String:Any]) -> Void) {
        if let activeMeeting = self.activeMeeting {
            if let info = activeMeeting.rosterModel.getAttendee(attendeeId: userId) {
                completion([
                    "userName": info.attendeeName ?? "",
                    "userID": info.attendeeId,
                    "audioStatus": activeMeeting.getAudioStatus(info.attendeeId),
                    "videoStatus": activeMeeting.getVideoStatus(info.attendeeId),
                ])
            }
            else {
                completion([
                    "userName": "",
                    "userID": userId,
                    "audioStatus": 1,
                    "videoStatus": 1,
                ])
            }
        }
        else {
            completion([
                "userName": "",
                "userID": userId,
                "audioStatus": 1,
                "videoStatus": 1,
            ])
        }
    }
    
    @objc(prepareMeetingWithJson:completion:)
    func prepareMeetingWithJson(json: [String:Any]?, completion: @escaping (Bool) -> Void) {
        let option = CallKitOption.disabled
        requestRecordPermission { success in
            guard success else {
                completion(false)
                return
            }
            guard let jsonData = json else {
                DispatchQueue.main.async {
                    completion(false)
                }
                return
            }
            JoinRequestService.getSessionConfig(json: jsonData) { (meetingSessionConfig) in
                guard let meetingSessionConfig = meetingSessionConfig else {
                    DispatchQueue.main.async {
                        completion(false)
                    }
                    return
                }
                let meetingId = meetingSessionConfig.externalMeetingId ?? "123456"
                let externalUserIdArray = meetingSessionConfig.credentials.externalUserId.components(separatedBy: "#")
                let selfName: String = externalUserIdArray.count >= 2 ? externalUserIdArray[1] : meetingSessionConfig.credentials.externalUserId
                let meetingModel = MeetingModel(meetingSessionConfig: meetingSessionConfig,
                                                meetingId: meetingId,
                                                selfName: selfName,
                                                callKitOption: option)
                self.meetings[meetingModel.uuid] = meetingModel

                switch option {
                case .incoming:
                    guard let call = meetingModel.call else {
                        completion(false)
                        return
                    }
                    let backgroundTaskIdentifier = UIApplication.shared.beginBackgroundTask(expirationHandler: nil)
                    DispatchQueue.main.asyncAfter(deadline: .now() + incomingCallKitDelayInSeconds) {
                        CallKitManager.shared().reportNewIncomingCall(with: call)
                        UIApplication.shared.endBackgroundTask(backgroundTaskIdentifier)
                    }
                case .outgoing:
                    guard let call = meetingModel.call else {
                        completion(false)
                        return
                    }
                    CallKitManager.shared().startOutgoingCall(with: call)
                case .disabled:
                    DispatchQueue.main.async { [weak self] in
                        self?.selectDevice(meetingModel, completion: completion)
                    }
                }
            }
        }
    }

    
    func selectDevice(_ meeting: MeetingModel, completion: @escaping (Bool) -> Void) {
        // This is needed to discover bluetooth devices
        configureAudioSession()
        // Phunv
        self.activeMeeting = meeting
        completion(true)
        MeetingModule.shared().deviceSelected(meeting.deviceSelectionModel)
//        self.meetingPresenter.showDeviceSelectionView(meetingModel: meeting) { success in
//            if success {
//                self.activeMeeting = meeting
//            }
//            completion(success)
//        }
    }

    func deviceSelected(_ deviceSelectionModel: DeviceSelectionModel) {
        guard let activeMeeting = activeMeeting else {
            return
        }
        activeMeeting.deviceSelectionModel = deviceSelectionModel
        // Phunv
        activeMeeting.startMeeting()
//        meetingPresenter.dismissActiveMeetingView {
//            self.meetingPresenter.showMeetingView(meetingModel: activeMeeting) { _ in }
//        }
    }

    func joinMeeting(_ meeting: MeetingModel, completion: @escaping (Bool) -> Void) {
        endActiveMeeting {
            self.meetingPresenter.showMeetingView(meetingModel: meeting) { success in
                if success {
                    self.activeMeeting = meeting
                }
                completion(success)
            }
        }
    }

    func getMeeting(with uuid: UUID) -> MeetingModel? {
        return meetings[uuid]
    }

    @objc(endActiveMeeting:)
    func endActiveMeeting(completion: @escaping () -> Void) {
        if let activeMeeting = activeMeeting {
            activeMeeting.endMeeting()
            meetingPresenter.dismissActiveMeetingView {
                self.meetings[activeMeeting.uuid] = nil
                self.activeMeeting = nil
                completion()
            }
        } else {
            completion()
        }
    }
    
    func dismissMeeting(_ meeting: MeetingModel) {
        if let activeMeeting = activeMeeting, meeting.uuid == activeMeeting.uuid {
            meetingPresenter.dismissActiveMeetingView(completion: {})
            meetings[meeting.uuid] = nil
            self.activeMeeting = nil
        } else {
            meetings[meeting.uuid] = nil
        }
    }

    func requestRecordPermission(completion: @escaping (Bool) -> Void) {
        let audioSession = AVAudioSession.sharedInstance()
        switch audioSession.recordPermission {
        case .denied:
            logger.error(msg: "User did not grant audio permission, it should redirect to Settings")
            completion(false)
        case .undetermined:
            audioSession.requestRecordPermission { granted in
                if granted {
                    completion(true)
                } else {
                    self.logger.error(msg: "User did not grant audio permission")
                    completion(false)
                }
            }
        case .granted:
            completion(true)
        @unknown default:
            logger.error(msg: "Audio session record permission unknown case detected")
            completion(false)
        }
    }

    func requestVideoPermission(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .denied, .restricted:
            logger.error(msg: "User did not grant video permission, it should redirect to Settings")
            completion(false)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { authorized in
                if authorized {
                    completion(true)
                } else {
                    self.logger.error(msg: "User did not grant video permission")
                    completion(false)
                }
            }
        case .authorized:
            completion(true)
        @unknown default:
            logger.error(msg: "AVCaptureDevice authorizationStatus unknown case detected")
            completion(false)
        }
    }

    func configureAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()
        do {
            if audioSession.category != .playAndRecord {
                try audioSession.setCategory(AVAudioSession.Category.playAndRecord,
                                             options: AVAudioSession.CategoryOptions.allowBluetooth)
                try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
            }
            if audioSession.mode != .voiceChat {
                try audioSession.setMode(.voiceChat)
            }
        } catch {
            logger.error(msg: "Error configuring AVAudioSession: \(error.localizedDescription)")
        }
    }
}