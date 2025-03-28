import os
import sys
import re
from shapely.geometry import Polygon,Point
from matplotlib import pylab as plt
import numpy as np
import openslide
from os.path import exists

slidename = sys.argv[1]
slnum = slidename.replace(".svs",'')
path1 = os.getcwd()+'/Block_analysis/annotation locations/'+slnum+'.svs.txt'
path2 = os.getcwd()+'/Block_analysis/patches/'+slnum+'.svs_patches.txt'

clo=0
par=0
pat=0

coords=[]
patc={}
npat={}
psize={}

def check_file_exists(file_path):
	return os.path.exists(file_path)
	
if check_file_exists(path1):
	file1 = open(path1)
	for f in file1:
		f = f.rstrip()
		fs = f.split('\t')
		if len(fs) > 3:
			if re.search("Clone",fs[4]):
				clo=clo+1
				coords.append([float(fs[7]),float(fs[8])])
			if re.search("Partial",fs[4]):
				#print ("This case has partials - partial counter will run")
				par=par+1
				coords.append([float(fs[7]),float(fs[8])])
			if re.search("Patch",fs[4]):
				pat=pat+1
		if re.search("calibration",f):
			k = re.findall('\d*\.?\d+',f)
			pix=float(k[0])
else:
	sys.exit()


fe = exists(path2)
if (fe == True):
	file2 = open(path2)
	for f2 in file2:
 		f2 = f2.rstrip()
 		out = re.split(r'\[', f2)
 		res = list(filter(None,out))
 		n=0
 		while (n<len(res)):
 			patc.update({n+1:res[n]})
 			npat.update({n+1:''})
 			psize.update({n+1:0})
 			n=n+1
 		


for k in patc:
	addon=[]
	patc[k] = patc[k].replace(']','')
	patc[k] = patc[k].replace('Point:','')
	patc[k] = patc[k].replace(' ','')
	spl = patc[k].split(',')
	a=0
	while a<len(spl):
		if ((a+1)%2) !=0:
			x=float(spl[a])*pix
		else:
			y=float(spl[a])*pix
			addon.append((x,y))
			npat[k] = addon
		a=a+1

for k in npat:
	pch = Polygon(npat[k])
	for c in coords:
		po = Point(c)
		if pch.contains(po):
			psize[k] = psize[k]+1
			
a = clo+par
val = psize.values()
total = sum(val)
b = pat+(clo-total)+par

ls=[]
for k in psize:
	e = str(psize[k])
	ls.append(e)
s=','
c = s.join(ls)

if par==0:
	cop = str(str(a)+"\t"+str(b)+"\t"+str(pat)+'\t'+str(par)+'\t'+c+'\t'+str(par))
	print (str(a)+"\t"+str(b)+"\t"+str(pat)+'\t'+str(par)+'\t'+c+'\t'+str(par)+'\t'+slnum)
	#pyperclip.copy(cop)

slide1 = openslide.OpenSlide(slidename)
size = 400
offest = (size/2)
finals = []

### go through annotations, image the partial and write the score
o = open(path1, "r")
for line in o:
	if (re.search("Partial",line)):
		entry = line.split("\t")
		xcen= float(entry[7])
		ycen= float(entry[8])
		x = int((xcen / pix) - offest)
		y = int((ycen / pix) - offest)
		img_region = slide1.read_region([x,y],0	,[size,size])
		plt.imshow(img_region)
		plt.draw()
		plt.pause(0.5)
		score = input()
		finals.append(score)
		plt.close()

### write the partial scores with commas 
joined = ",".join(finals)

if par!=0:
	cop2 = str((str(a)+"\t"+str(b)+"\t"+str(pat)+'\t'+str(par)+'\t'+c+'\t'+str(joined)))
	print (str(a)+"\t"+str(b)+"\t"+str(pat)+'\t'+str(par)+'\t'+c+'\t'+str(joined)+'\t'+slnum)
	#pyperclip.copy(cop2)
