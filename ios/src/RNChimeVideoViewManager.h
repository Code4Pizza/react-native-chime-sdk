//
//  RNChimeVideoViewManager.h
//  react-native-chime-sdk
//
//  Created by Phu on 6/18/21.
//

#import <UIKit/UIKit.h>
#import <React/RCTViewManager.h>

NS_ASSUME_NONNULL_BEGIN

@interface UserVideoView : UIView

- (void)showVideoAttendeeId:(NSString *)attendeeId;

@end

@interface ChimeShareView : UIView

@end

@interface RNChimeVideoViewManager : RCTViewManager

@end

@interface RNChimeShareViewManager : RCTViewManager

@end

NS_ASSUME_NONNULL_END
