import { NativeModules } from 'react-native';

type ChimeSdkType = {
  multiply(a: number, b: number): Promise<number>;
};

const { ChimeSdk } = NativeModules;

export default ChimeSdk as ChimeSdkType;
