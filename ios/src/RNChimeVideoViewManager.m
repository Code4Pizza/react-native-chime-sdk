//
//  RNChimeVideoViewManager.m
//  react-native-chime-sdk
//
//  Created by Phu on 6/18/21.
//

#import "RNChimeVideoViewManager.h"

@interface RNChimeView: UIView {
}

@property (nonatomic, strong) UserVideoView *userVideo;

- (void) setUserID: (NSString *) userID;

@end

@implementation RNChimeView

- (id)initWithFrame:(CGRect)frame;
{
    self = [super initWithFrame:frame];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (id)initWithCoder:(NSCoder *)aDecoder;
{
    self = [super initWithCoder:aDecoder];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit
{
    self.userVideo = [[UserVideoView alloc] initWithFrame:self.bounds];
    [self addSubview:self.userVideo];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.userVideo.frame = self.bounds;
}

- (void) setUserID: (NSString *) userID {
    [self.userVideo showVideoAttendeeId:userID];
}

- (void) dealloc {
}

@end

@implementation RNChimeVideoViewManager

RCT_EXPORT_VIEW_PROPERTY(userID, NSString);

RCT_EXPORT_MODULE(RNChimeVideoView)

- (UIView *)view
{
    RNChimeView *view = [[RNChimeView alloc] initWithFrame:CGRectZero];
    return view;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

@end
