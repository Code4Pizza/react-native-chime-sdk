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
    var currentAttendeeId: String?
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        videoView.frame = self.bounds
    }
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.clipsToBounds = true
        videoView.contentMode = .scaleAspectFill
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
            if let videoTileState =
                MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(attendeeId) {
                
                MeetingModule.shared().activeMeeting?.bind(videoRenderView: self.videoView, tileId: videoTileState.tileId)
            }
        }
    }
    func stopVideoAttendee(_ sendNotification: Bool = false) {
        if let currentAttendeeId = currentAttendeeId,
           currentAttendeeId.count > 0
        {
            if let videoTileState = MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(currentAttendeeId)
            {
                MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.unbindVideoView(tileId: videoTileState.tileId)
                if sendNotification {
                    NotificationCenter.default.post(name: Notification.Name("onUserVideoStatusChangedChime"), object: nil, userInfo: ["userID" : videoTileState.attendeeId])
                }
            }
            self.currentAttendeeId = ""
        }
    }
    deinit {
        NotificationCenter.default.removeObserver(self)
        self.stopVideoAttendee(true)
    }
    
}
