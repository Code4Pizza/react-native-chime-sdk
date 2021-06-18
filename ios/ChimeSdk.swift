@objc(ChimeSdk)
class ChimeSdk: NSObject {

    @objc(multiply:withB:withResolver:withRejecter:)
    func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        resolve(a*b)
    }
    
    @objc(getJsonMeeting:withUserName:withResolver:withRejecter:)
    func getJsonMeeting(meetingId: String, userName: String, resolve: @escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        MeetingModule.shared().getJsonMeeting(meetingId: meetingId, selfName: userName, option: .disabled, overriddenEndpoint: "") { (json) in
            resolve(json)
        }
    }
    
    @objc(startMeeting:withResolver:withRejecter:)
    func startMeeting(json: [String:Any], resolve: @escaping RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        MeetingModule.shared().prepareMeetingWithJson(json: json, option: .disabled) { (success) in
            resolve(success)
        }
    }
    
    @objc(endActiveMeeting)
    func endActiveMeeting() -> Void {
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
