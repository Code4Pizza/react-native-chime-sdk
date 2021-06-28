import * as React from 'react';
import { useEffect, useRef, useState } from 'react';

  }, []);

  const join = () => {
    joinMeeting({
      meetingUrl: 'http://6dffed99a61f.ngrok.io/',
      meetingId: 'klplpsssp',
      attendeeName: 'The Anh',
    });
  };

  const leave = () => {
    leaveCurrentMeeting();
  };

  const getParty = async () => {
    let data = await getParticipants();
    // @ts-ignore
    data.members.forEach((user) => {
      console.log(user.userID + '-' + user.userName);
    });
    // etUserList(data.members);
  };

  // @ts-ignore
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const onViewableItemChanged = useRef(({ viewableItems, changed }) => {
    let maps = new Map();
    viewableItems.map((item: { index: any }) => {
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
            // @ts-ignore
            key={item.userID}
            onPress={async () => {
              // let { info } = await getUserInfoZoom(item.userID);
              // console.log(info.userName + '-' + info.avatarPath);
            }}
          >
            <View
              style={{
                width: 300,
                height: 300,
                borderRadius: 30,
                overflow: 'hidden',
                margin: 16,
              }}
            >
              <RNChimeView
                style={{ width: 300, height: 300, borderRadius: 30 }}
                // @ts-ignore
                userID={map.has(index) ? item.userID : ''}
              />
            </View>
          </TouchableOpacity>
        )}
        // @ts-ignore
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
