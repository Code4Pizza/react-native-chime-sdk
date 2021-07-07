#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

typedef void (^RCTPromiseResolveBoolBlock)(BOOL success);
typedef void (^RCTPromiseResolveVoidBlock)(void);
typedef void (^RCTPromiseResolveArrayBlock)(NSArray *list);
typedef void (^RCTPromiseResolveObjectBlock)(NSDictionary *object);

@interface MeetingModule : NSObject

+ (MeetingModule *) shared;
- (void) prepareMeetingWithJson: (NSDictionary *) meetingJson completion:(RCTPromiseResolveBoolBlock)resolve;
- (void) endActiveMeeting:(RCTPromiseResolveVoidBlock)resolve;
- (void) setRCTEventEmitter:(RCTEventEmitter *) eventEmitter;
- (void) onMyAudio;
- (void) offMyAudio;
- (void) onOffMyVideo;
- (void) getParticipants:(RCTPromiseResolveArrayBlock)resolve;
- (void) getUserInfo:(NSString *)userId completion:(RCTPromiseResolveObjectBlock)resolve;
- (void) getListAudioDevices:(RCTPromiseResolveArrayBlock)resolve;
- (void) getListVideoDevices:(RCTPromiseResolveArrayBlock)resolve;
- (void) selectAudioDevice: (NSDictionary *) mediaDevice;
- (void) selectVideoDevice: (NSDictionary *) mediaDevice;
@end

@interface ChimeSdk : RCTEventEmitter <RCTBridgeModule>
@end
