#! /usr/env/python

import serial
import pygame
import pygame.camera
import sys
import os
import time


def main():
  # check args
  if(len(sys.argv) < 2):
    print "you didn't provide enough args, you idiot"
    print "%s /dev/tty.something [/log/folder/path]" % sys.argv[0]
    sys.exit()

  w= 640
  h= 480
  size=(w,h)
  screen = pygame.display.set_mode(size)
  # get serial connection
  gps = serial.Serial(sys.argv[1], 9600, timeout=1)

  # init camera
  pygame.init()
  pygame.camera.init()
  camlist = pygame.camera.list_cameras()
  print 'avilable Camreas:' + str(camlist)+'\n'
  camToUse = "/dev/video1"
  cam =pygame.camera.Camera(camToUse,(640,480))
  print 'useing'+camToUse+'\n'
  cam.start()

  while True:
   print "press enter to make a new data point"
   raw_input()
   img = cam.get_image()
   screen.blit(img,(0,0))
   # make a new folder
   start_time = time.time()
   log_folder = str(start_time)
   os.mkdir(log_folder)

   # collect an image
 
   filename = "%s/image.jpg" % (log_folder)
   pygame.image.save(img, filename)
   image=pygame.image.load(filename) 
   screen.blit(image,(0,0))
   pygame.display.update()
   # collect gps data for 10 seconds
   # gps.flushInput()
   with open("%s/gpslog.txt" % (log_folder), "w") as gpslog_file:
      while(time.time() < start_time + 60):
        gps_line = gps.readline()
        print gps_line
        gpslog_file.write(gps_line)





if __name__ == "__main__":
  main()
