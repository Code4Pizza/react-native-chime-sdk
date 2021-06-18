import * as React from 'react';

import { StyleSheet, View, Text, Button, requireNativeComponent, FlatList, SafeArea} from 'react-native';
import ChimeSdk from 'react-native-chime-sdk';
const NativeChimeVideoView = requireNativeComponent('RNChimeVideoView');

export default function App() {
  const [members, setMembers] = React.useState([]);

  React.useEffect(() => {
  }, []);

  const joinMeeting = React.useCallback(() => {
    ChimeSdk.getJsonMeeting("654321", "nguyenphu2810@gmail.com").then( (json) => {
        console.log("+++ ok result ", json);
        if (json) {
          ChimeSdk.startMeeting(json).then((success) => {
            console.log("+++ ok result success ", success);
          });
        }
      }
    );
  }, []);

  const leaveMeeting = React.useCallback(() => {
    ChimeSdk.endActiveMeeting();
  }, []);

  const getParticipants = React.useCallback(() => {
    ChimeSdk.getParticipants().then((list) => {
      setMembers(list);
      console.log('+++ participants ', list);
    });
  }, []);

  const renderItem = React.useCallback(({item}) => {
    return (
      <NativeChimeVideoView
        style={{width: 100, height: 100}}
        userID={item.zoomId}
      />
    );
  }, []);

  return (
    <View style={styles.container}>
      <View style={{height: 50}}></View>
      <Button
        onPress={joinMeeting}
        title="Join"
        color="#841584"
        accessibilityLabel="Learn more about this purple button"
      />
      <Button
        onPress={leaveMeeting}
        title="Leave"
        color="#841584"
        accessibilityLabel="Learn more about this purple button"
      />
      <Button
        onPress={getParticipants}
        title="Participants"
        color="#841584"
        accessibilityLabel="Learn more about this purple button"
      />
      <FlatList
        style={{flex: 1}}
        data={members}
        renderItem={renderItem}
        keyExtractor={item => item.userName}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
