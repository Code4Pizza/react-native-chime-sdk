//
//  AppDelegate.swift
//  AmazonChimeSDKDemo
//
//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: Apache-2.0
//

import UIKit
import AmazonChimeSDK

@objc(UserVideoView)
public class UserVideoView: UIView {
    
    var videoView = DefaultVideoRenderView(frame: .zero)
    var isBind = false
    var currentAttendeeId: String?
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        videoView.frame = self.bounds
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.clipsToBounds = true
        videoView.contentMode = .scaleAspectFit
        NotificationCenter.default.addObserver(self, selector: #selector(handleVideoStatusChange), name: Notification.Name("onUserVideoStatusChangedChime"), object: nil)
        self.addSubview(videoView)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    @objc private func handleVideoStatusChange(notification: NSNotification){
        if let userInfo = notification.userInfo,
           let userID = userInfo["userID"] as? String,
           let currentId = currentAttendeeId,
           userID == currentId
        {
            self.bindVideo(currentId)
        }
    }
    @objc(showVideoAttendeeId:)
    func showVideoAttendeeId(_ attendeeId: String) {
        if self.currentAttendeeId != attendeeId {
            self.stopVideoAttendee()
            self.bindVideo(attendeeId)
            self.currentAttendeeId = attendeeId
        }
        else {
            if self.currentAttendeeId?.count ?? 0 > 0 {
                self.bindVideo(self.currentAttendeeId!)
            }
        }
    }
    func bindVideo(_ attendeeId: String) {
        if attendeeId.count > 0
        {
            self.videoView.isHidden = false
            if let videoTileState =
                MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(attendeeId) {
                if let attendee = MeetingModule.shared().activeMeeting?.rosterModel.getAttendee(attendeeId: attendeeId) {
                    print("+++ bind video ", attendee.attendeeName);
                }
                //if videoTileState.pauseState == .unpaused {
                MeetingModule.shared().getTileQueue(videoTileState.tileId).addOperation (
                    SynchronousOperation { _ in
                        Thread.sleep(forTimeInterval: 0.10)
                        MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.resumeRemoteVideoTile(tileId: videoTileState.tileId)
                    }
                )
                
                MeetingModule.shared().activeMeeting?.bind(videoRenderView: self.videoView, tileId: videoTileState.tileId)
//                }
//                else {
//                    self.videoView.isHidden = true
//                }
                isBind = true
            }
            else {
                self.videoView.isHidden = true
            }
        }
        else {
            self.videoView.isHidden = true
        }
    }
    func stopVideoAttendee(_ sendNotification: Bool = false) {
        if let currentAttendeeId = currentAttendeeId,
           currentAttendeeId.count > 0
        {
            if let videoTileState = MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(currentAttendeeId)
            {
                self.videoView.isHidden = true
                self.currentAttendeeId = ""
                //MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.unbindVideoView(tileId: videoTileState.tileId)
                //MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.pauseRemoteVideoTile(tileId: videoTileState.tileId)
                MeetingModule.shared().getTileQueue(videoTileState.tileId).addOperation (
                    SynchronousOperation { _ in
                        Thread.sleep(forTimeInterval: 0.10)
                        MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.pauseRemoteVideoTile(tileId: videoTileState.tileId)
                    }
                )
                if sendNotification {
                    //let deadlineTime = DispatchTime.now() + .seconds(6)
                    DispatchQueue.main.async {
                        NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : videoTileState.attendeeId])
                    }
                    
                }
                //videoView.resetImage()
                isBind = false
            }
        }
        else {
            self.videoView.isHidden = true
        }
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
        self.stopVideoAttendee(true)
        videoView.removeFromSuperview()
    }
    
}


@objc(ChimeShareView)
public class ChimeShareView: UIView {
    
    var videoView = DefaultVideoRenderView(frame: .zero)
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        videoView.frame = self.bounds
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.clipsToBounds = true
        videoView.contentMode = .scaleAspectFit
        NotificationCenter.default.addObserver(self, selector: #selector(handleShareStatusChange), name: Notification.Name("onShareVideoStatusChangedChime"), object: nil)
        self.addSubview(videoView)
        print("+++ create share view")
        self.bindShareVideo()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    @objc private func handleShareStatusChange(notification: NSNotification){
        self.bindShareVideo()
    }
    func bindShareVideo() {
        if let activeMeeting = MeetingModule.shared().activeMeeting,
           activeMeeting.screenShareModel.isAvailable,
           let tileId = activeMeeting.screenShareModel.tileId
        {
            print("+++ bind share video \(tileId)")
            self.videoView.isHidden = false
            activeMeeting.currentMeetingSession.audioVideo.bindVideoView(videoView: self.videoView, tileId: tileId)
        }
        else {
            self.videoView.isHidden = true
        }
    }
    func stopBindVideo() {
        self.videoView.isHidden = true
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
        self.stopBindVideo()
    }
    
}
