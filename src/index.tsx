import React from 'react';
import {
  NativeModules,
  requireNativeComponent,
  NativeEventEmitter,
} from 'react-native';

const { ChimeSdk } = NativeModules;
const eventEmitter = new NativeEventEmitter(ChimeSdk);

const NativeChimeView = requireNativeComponent('RNChimeVideoView');
const NativeChimeShareView = requireNativeComponent('RNChimeShareView');

export const initSdk = () => {};

export const joinMeeting = (meetingInfo: object) => {
  ChimeSdk.joinMeeting(meetingInfo);
};

export const leaveCurrentMeeting = () => {
  ChimeSdk.leaveCurrentMeeting();
};

export const listAudioDevices = async () => {
  return await ChimeSdk.listAudioDevices();
};

export const selectAudioDevice = (mediaDevice: object) => {
  ChimeSdk.selectAudioDevice(mediaDevice);
};

export const onMyAudio = () => {
  ChimeSdk.onMyAudio();
};

export const offMyAudio = () => {
  ChimeSdk.offMyAudio();
};

export const listVideoDevices = async () => {
  return await ChimeSdk.listVideoDevices();
};

export const selectVideoDevice = (mediaDevice: object) => {
  ChimeSdk.selectVideoDevice(mediaDevice);
};

export const switchCamera = () => {
  ChimeSdk.switchCamera();
};

export const onOffMyVideo = () => {
  ChimeSdk.onOffMyVideo();
};

export const getParticipants = () => {
  return new Promise((res) => {
    ChimeSdk.getParticipants((members: any) => {
      return res({ error: false, members });
    });
  });
};

export const getUserInfo = (userID: string) => {
  return new Promise((res) => {
    ChimeSdk.getUserInfo(userID, (info: any) => {
      return res({ error: false, info });
    });
  });
};

export const onEventListener = (onEvent = () => {}) => {
  eventEmitter.addListener('onChimeMeetingEvent', onEvent);
};

export const removeListener = () => {
  eventEmitter.removeAllListeners('onChimeMeetingEvent');
};

const RNChimeView = (props: any) => {
  return (
    <NativeChimeView
      // @ts-ignore
      style={props.style}
      userID={props.userID}
    />
  );
};

export const RNChimeShareView = (props: any) => {
  return (
    <NativeChimeShareView
      // @ts-ignore
      style={props.style}
    />
  );
};

export default RNChimeView;
