import * as React from 'react';
import { useEffect, useRef, useState } from 'react';

import {
  StyleSheet,
  View,
  Text,
  Button,
  TouchableOpacity,
  FlatList,
} from 'react-native';

import {
  ChimeView,
  joinMeeting,
  leaveCurrentMeeting,
  onMyAudio,
  offMyAudio,
  getParticipants,
  getUserInfo,
  onEventListenerZoom,
  removeListenerZoom,
} from 'react-native-chime-sdk';

export default function App() {
  const [status, setStatus] = useState('Idle');

  const [map, setMap] = useState(new Map());
  const [userList, setUserList] = useState([]);

  useEffect(() => {
    onEventListenerZoom(async (data) => {
      if (data.event === 'meetingStateChange') {
        setStatus(data.des);
        if (data.des === 'meeting_ready') {
          console.log('In meeting, get participants');
          let rs = await getParticipants();
          let list = [...rs.members];
          // for (let i = 0; i < 5; i++) {
          //   list.push({
          //     userID: '0',
          //     userName: 'RossBlueBerry',
          //   });
          // }
          setUserList(list);
        }
        if (data.des === 'idle') {
          setUserList([]);
          setStatus('Idle');
        }
      }

      if (data.event === 'sinkMeetingUserJoin') {
        console.log(data.event + ' : ' + data.userID);
        if (!userList.includes(data.userID)) {
          setUserList((old) => {
            return [data].concat(old);
            // return [...old, data.userID];
          });
        }
      }

      if (data.event === 'sinkMeetingUserLeft') {
        console.log(data.event + ' : ' + data.userID);
        setUserList((old) => {
          return old.filter((item) => item.userID !== data.userID);
        });
      }
    });
    return () => {
      removeListenerZoom;
    };
  }, []);

  const join = () => {
    joinMeeting({
      meetingUrl: 'http://de0671673fc8.ngrok.io/',
      meetingId: 'klplpp',
      attendeeName: 'The Anh',
    });
  };

  const leave = () => {
    leaveCurrentMeeting();
  };

  const getParty = async () => {
    let data = await getParticipants();
    data.members.forEach((user) => {
      console.log(user.userID + '-' + user.userName);
    });
    // etUserList(data.members);
  };

  const onViewableItemChanged = useRef(({ viewableItems, changed }) => {
    let maps = new Map();
    viewableItems.map((item) => {
      maps.set(item.index, true);
    });
    setMap(maps);
  });

  return (
    <View style={styles.container}>
      {/* <Text>Result: {result}</Text> */}
      <View style={[{ width: '90%', margin: 5, backgroundColor: 'red' }]}>
        <Button title="Join" onPress={join} color="#FFAa00" />
      </View>
      <View style={{ height: 5 }} />
      <View style={[{ width: '90%', margin: 5, backgroundColor: 'red' }]}>
        <Button title="Leave" onPress={leave} color="#FF3D00" />
      </View>
      <View style={{ height: 5 }} />
      <Button title="Get party" onPress={getParty} />
      <View style={{ height: 5 }} />
      <Text style={{ color: '#f44336' }}>{status}</Text>
      <FlatList
        horizontal={true}
        onViewableItemsChanged={onViewableItemChanged.current}
        data={userList}
        renderItem={({ item, index }) => (
          <TouchableOpacity
            key={item.userID}
            onPress={async () => {
              // let { info } = await getUserInfoZoom(item.userID);
              // console.log(info.userName + '-' + info.avatarPath);
            }}
          >
            <View
              style={{
                width: 120,
                height: 120,
                borderRadius: 30,
                overflow: 'hidden',
                margin: 16,
              }}
            >
              <ChimeView
                style={{ width: 120, height: 120, borderRadius: 30 }}
                userID={map.has(index) ? item.userID : ''}
              />
            </View>
          </TouchableOpacity>
        )}
        keyExtractor={(item, index) => index.toString()}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    // justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
