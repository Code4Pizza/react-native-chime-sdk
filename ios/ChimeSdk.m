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
RCT_EXPORT_METHOD(listAudioDevices:(RCTResponseSenderBlock)callback)
{
    [[MeetingModule shared] getListAudioDevices:^(NSArray *list) {
        callback(@[list]);
    }];
}
RCT_EXPORT_METHOD(listVideoDevices:(RCTResponseSenderBlock)callback)
{
    [[MeetingModule shared] getListVideoDevices:^(NSArray *list) {
        callback(@[list]);
    }];
}
RCT_EXPORT_METHOD(selectAudioDevice:(NSDictionary *) mediaDevice)
{
    [[MeetingModule shared] selectAudioDevice:mediaDevice];
}
RCT_EXPORT_METHOD(selectVideoDevice:(NSDictionary *) mediaDevice)
{
    [[MeetingModule shared] selectVideoDevice:mediaDevice];
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

