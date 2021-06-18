//
//  AppDelegate.swift
//  AmazonChimeSDKDemo
//
//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: Apache-2.0
//

import UIKit
import AmazonChimeSDK

//@UIApplicationMain
//class AppDelegate: UIResponder, UIApplicationDelegate {
//    var window: UIWindow?
//
//    func application(_: UIApplication,
//                     didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
////        window?.rootViewController = UIStoryboard(name: "LaunchScreen", bundle: nil).instantiateInitialViewController()
////        window?.makeKeyAndVisible()
////
////        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 3) {
////            self.window?.rootViewController = UIStoryboard(name: "Main", bundle: nil).instantiateInitialViewController()
////        }
//
//        window = UIWindow(frame: UIScreen.main.bounds)
//        let rootVC = DemoViewController()
//        let rootNC = UINavigationController(rootViewController: rootVC)
//        window?.rootViewController = rootNC
//        window?.makeKeyAndVisible()
//
//        return true
//    }
//
//    func applicationWillTerminate(_ application: UIApplication) {
//        // If app is being force killed while CallKit integrated meeting is in progress,
//        // provider(_: CXProvider, perform action: CXEndCallAction) will not be called.
//        // So we need to invoke isEndedHandler() directly.
//        if let call = MeetingModule.shared().activeMeeting?.call {
//            call.isEndedHandler?()
//        } else {
//            MeetingModule.shared().endActiveMeeting {}
//        }
//    }
//
//    func applicationDidEnterBackground(_ application: UIApplication) {
//        if let meeting = MeetingModule.shared().activeMeeting {
//            meeting.isAppInBackground = true
//        }
//    }
//
//    func applicationWillEnterForeground(_ application: UIApplication) {
//        if let meeting = MeetingModule.shared().activeMeeting {
//            meeting.isAppInBackground = false
//        }
//    }
//}
//
class DemoViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    let meetingIdTf = UITextField()
    let joinBtn = UIButton()
    var list: UITableView!

    override func viewDidLoad() {
        super.viewDidLoad()

        self.navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .cancel, target: self, action: #selector(handleEnd))

        self.navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .refresh, target: self, action: #selector(handleRefresh))

        self.view.backgroundColor = UIColor.white

        self.view.addSubview(meetingIdTf)
        self.view.addSubview(joinBtn)

        meetingIdTf.frame = CGRect(x: 0, y: 64, width: 200, height: 40);
        meetingIdTf.placeholder = "MeetingID"

        joinBtn.frame = CGRect(x: 200, y: 64, width: 100, height: 40)
        joinBtn.setTitle("Join", for: .normal)
        joinBtn.backgroundColor = UIColor.blue

        joinBtn.addTarget(self, action: #selector(handleJoin), for: .touchUpInside)

        list = UITableView(frame: CGRect(x: 0, y: 104, width: self.view.bounds.width, height: self.view.bounds.height - 104))
        list.rowHeight = 100
        list.register(UserCell.self, forCellReuseIdentifier: "UserCell")
        list.dataSource = self
        list.delegate = self
        self.view.addSubview(list)
    }
    @objc func handleEnd() {
        if let activeMeeting = MeetingModule.shared().activeMeeting {
            activeMeeting.endMeeting()
            MeetingModule.shared().dismissMeeting(activeMeeting)
        }
    }
    @objc func handleRefresh() {
        list.reloadData()
    }
    @objc func handleJoin() {
        MeetingModule.shared().prepareMeeting(meetingId: meetingIdTf.text ?? "123456",
                                              selfName: "nguyenphu2810@gmail.com",
                                              option: .disabled,
                                              overriddenEndpoint: "") { success in
        }
    }
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let userData = MeetingModule.shared().activeMeeting?.rosterModel.getAttendee(at: indexPath.row),
           let videoTileState = MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(userData.attendeeId) {
            MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.resumeRemoteVideoTile(tileId: videoTileState.tileId)
        }
    }
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return MeetingModule.shared().activeMeeting?.rosterModel.getTotalAttendee() ?? 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "UserCell", for: indexPath as IndexPath) as! UserCell
        //cell.textLabel!.text = "\(indexPath.row)"
        let userData = MeetingModule.shared().activeMeeting?.rosterModel.getAttendee(at: indexPath.row)
        cell.name.text = "\(userData?.attendeeName)"
        cell.videoView.showVideoAttendeeId(userData?.attendeeId ?? "")
        return cell
    }

}

class UserCell: UITableViewCell {

    var name = UILabel();
    var videoView = UserVideoView(frame: CGRect(x: 0, y: 30, width: 70, height: 70))

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        name.frame = CGRect(x: 0, y: 0, width: 300, height: 30)

        contentView.addSubview(name)
        contentView.addSubview(videoView)
    }

    required init(coder aDecoder: NSCoder) {
         fatalError("init(coder:) has not been implemented")
    }

}

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
        self.addSubview(videoView)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc(showVideoAttendeeId:)
    func showVideoAttendeeId(_ attendeeId: String) {
        if self.currentAttendeeId != attendeeId {
            self.stopVideoAttendee()
            if attendeeId.count > 0
            {
                if let videoTileState =
                    MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(attendeeId) {
                    MeetingModule.shared().activeMeeting?.bind(videoRenderView: self.videoView, tileId: videoTileState.tileId)
                    MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.resumeRemoteVideoTile(tileId: videoTileState.tileId)
                }
            }
            self.currentAttendeeId = attendeeId
        }
    }
    func stopVideoAttendee() {
        if let currentAttendeeId = currentAttendeeId,
           currentAttendeeId.count > 0
        {
            if let videoTileState = MeetingModule.shared().activeMeeting?.videoModel.getRemoteVideoTileState(currentAttendeeId)
            {
                MeetingModule.shared().activeMeeting?.currentMeetingSession.audioVideo.pauseRemoteVideoTile(tileId: videoTileState.tileId)
            }
            self.currentAttendeeId = ""
        }
    }
    deinit {
        self.stopVideoAttendee()
    }
}
