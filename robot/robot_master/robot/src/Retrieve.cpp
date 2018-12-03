//
// Created by andrei on 03/03/18.
//

#include "ros/ros.h"
#include "std_msgs/String.h"
#include <sstream>
#include <fstream>
#include <queue>

using namespace std;

int main(int argc, char **argv)
{
    ros::init(argc, argv, "retriever");
    ros::NodeHandle n;
    ros::Publisher retriever_pub = n.advertise<std_msgs::String>("retriever", 1000);

    ros::Rate loop_rate(10);
    int count = 0;
    while (ros::ok())
    {
        ifstream inFile;
        inFile.open("commands.txt");
        std::string s;

        std_msgs::String msg;
        msg.data = "erreverv";
        retriever_pub.publish(msg);
        std::stringstream ss;

        if (inFile.is_open())
        {
            std::string line;
            while (std::getline(inFile, line)){
                ss << line;
                msg.data = ss.str();
                ROS_INFO("%s", msg.data.c_str());
                retriever_pub.publish(msg);
            }
            if (!inFile.eof()){
                /*std::ofstream ofs;
                ofs.open("commands.txt", std::ofstream::out | std::ofstream::trunc);
                ofs.close();*/
                break; // Ensure end of read was EOF.
            }
            inFile.clear();

            //usleep(3000000); // chack every 3 seconds
        }

        inFile.close();


        ros::spinOnce();

        loop_rate.sleep();
    }

}