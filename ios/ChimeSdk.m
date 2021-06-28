#import "ChimeSdk.h"

@implementation ChimeSdk

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(joinMeeting:(NSDictionary *) meetingInfo)
{
    [[MeetingModule shared] setRCTEventEmitter:self];
    [[MeetingModule shared] prepareMeetingWithJson:meetingInfo completion:^(BOOL success) {        
    }];
}

RCT_EXPORT_METHOD(leaveCurrentMeeting)
{
    [[MeetingModule shared] endActiveMeeting:^{
    }];
}
RCT_EXPORT_METHOD(onMyAudio)
{
    [[MeetingModule shared] onMyAudio];
}
RCT_EXPORT_METHOD(offMyAudio)
{
    [[MeetingModule shared] offMyAudio];
}

RCT_EXPORT_METHOD(onOffMyVideo)
{
    [[MeetingModule shared] onOffMyVideo];
}
RCT_EXPORT_METHOD(getParticipants:(RCTResponseSenderBlock)callback)
{
    [[MeetingModule shared] getParticipants:^(NSArray *list) {
        callback(@[list]);
    }];
}
RCT_EXPORT_METHOD(getUserInfo:(NSString *) userId callback:(RCTResponseSenderBlock)callback)
{
    [[MeetingModule shared] getUserInfo:userId completion:^(NSDictionary *object) {
        callback(@[object]);
    }];
}
- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
- (NSArray<NSString *> *)supportedEvents {
    return @[@"onChimeMeetingEvent"];
}

@end

