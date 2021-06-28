@objc(ChimeSdkSwift)
class ChimeSdkSwift: NSObject {
    
    @objc(getJsonMeeting:withUserName:withResolver:withRejecter:)
    func getJsonMeeting(meetingId: String, userName: String, resolve: @escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        MeetingModule.shared().getJsonMeeting(meetingId: meetingId, selfName: userName, option: .disabled, overriddenEndpoint: "") { (json) in
            resolve(json)
        }
    }
    
    @objc(joinMeeting:completion:)
    func joinMeeting(json: [String:Any], resolve: @escaping RCTPromiseResolveBlock) -> Void {
        MeetingModule.shared().prepareMeetingWithJson(json: json) { (success) in
            resolve(success)
        }
    }
    
    @objc(leaveCurrentMeeting)
    func leaveCurrentMeeting() -> Void {
        MeetingModule.shared().endActiveMeeting {            
        }
    }
    
    @objc(getParticipants:withRejecter:)
    func getParticipants(resolve: @escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        if let activeMeeting = MeetingModule.shared().activeMeeting {
            var list = [[String:Any]]();
            for i in 0..<activeMeeting.rosterModel.getTotalAttendee() {
                let model = activeMeeting.rosterModel.getAttendee(at: i)
                list.append([
                                "userName": model?.attendeeName,
                                "zoomId": model?.attendeeId]
                )
            }
            resolve(list)
        }
        else {
            resolve([])
        }
    }
}
