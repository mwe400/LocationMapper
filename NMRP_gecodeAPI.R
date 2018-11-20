#NMRP project - Spatial Data Analysis with Google's Geocoding API

#load libraries
library(dplyr)
library(stringr)
library(RJSONIO)
library(RCurl)
library(ggplot2)
library(ggmap)
library(mapview)   
library(sp)

#load the csv file into the workspace
GeoData <- read.csv(file.choose(), header = TRUE, sep =",")

#some data cleaning
DataSet <- GeoData
str(DataSet)
DataSet <- DataSet[, -1]

#changing the data types
DataSet$County <- as.character(DataSet$County)
DataSet$Location <- as.character(DataSet$Location)
str(DataSet)
summary(DataSet)

#consolidating the records based on County
CompositeData <- DataSet %>%
            select(County,Location,Frequency) %>%
            group_by(County, Location) %>%
            summarise(Frequency = sum(Frequency))

str(CompositeData)

#create a new varaible for the count of words in the location variable
CompositeData$word <-  vapply(strsplit(CompositeData$Location, "\\W+"), length, integer(1))

#Omit the locations which are having the word count higher than 10
CompositeData <- subset(CompositeData ,CompositeData$word < 6, select =  c(1:3))

FinalData <- CompositeData

View(FinalData)

#create new columns to call the geocode function
FinalData$State <- NA
FinalData$lat<-NA
FinalData$long<-NA
FinalData$formatted_address<-NA

#Create a new column to split the Statename from the county
FinalData$State <- as.data.frame(str_split_fixed(as.character(geo1$County), "-", 2))[,2]
FinalData$State <- trimws(geo1$State)

#Defining geocode function with the API-KEY
getGeoData <- function(location) {
  location <- gsub(' ','+',location)
  geo_data <- getURL(paste("https://maps.googleapis.com/maps/api/geocode/json?address=",location,"&key=AIzaSyA6FvTYYJ8vPdSse9ElrLWUeQa9pXJAC7o", sep=""))
  #geo_data <- getURL(paste("https://maps.googleapis.com/maps/api/geocode/json?address=",location,sep=""))
  raw_data_2 <- fromJSON(geo_data)
  return(raw_data_2)
}

#loop to generate the geographical coordinates of every record
#Pick the first location coordinates in case of one value
for(i in 1:nrow(FinalData))
{
  jsonData <- getGeoData(as.character(FinalData$Location[i]))
  #jsonData <- geocode(as.character(FinalData$Location[i]),output="all",override_limit=TRUE)
 
  print(geo1$Location[i])
  print(jsonData$status)
  if (jsonData$status == "OK") {
    if (length(jsonData$results) == 1) {
       formattedAddress <- jsonData$results[[1]]$formatted_address
       FinalData$formatted_address[i] <- formattedAddress
       #Focusing only on the locations pertaining to USA
       if(grepl("USA", formattedAddress)) {
         FinalData$lat[i] <- getElement(jsonData$results[[1]]$geometry$location,"lat")
         FinalData$long[i] <- getElement(jsonData$results[[1]]$geometry$location,"lng")
       }
       
       #If there are multiple locations, pick the location coordinates matching with its corresponding state
    } else {
      for(j in 1:length(jsonData$results)) {
        formattedAddress <- jsonData$results[[j]]$formatted_address
        FinalData$formatted_address[i] <- formattedAddress
        if(grepl("USA", formattedAddress)) {
          print(formattedAddress)
          if(grepl(paste(", ",as.character(geo1$State[i]),sep=""), formattedAddress)) {
            FinalData$lat[i] <- getElement(jsonData$results[[j]]$geometry$location,"lat")
            FinalData$long[i] <- getElement(jsonData$results[[j]]$geometry$location,"lng")
          }
        }
        
      }
      
    }
  } 
    
}

#check if the latitude and longitudinal coordinates populated
View(FinalData)

#get the USA map
USAMap = ggmap(get_map(location = geocode("United States"), scale= 1, zoom= 4,maptype = "roadmap"),  source = "google")
#USAMap

# Static visualization of the Location with respect to its location occurance frequency
USAMap+
geom_jitter(aes(x=long, y=lat, color = County), data=geo1, position = "jitter", size=geo1$Frequency*0.03, color = "blue", alpha = 0.3) +
scale_size_continuous(range=range(geo1$Frequency)) +geom_text(data = geo1, aes(x = long, y = lat, label = Frequency),
                                                                  size= 2, vjust = 1, hjust = 1, check_overlap = TRUE)



#To Create a interactive map

#omit the NA's records for geographical coordinates
geo1spat <- na.omit(FinalData)

#use sp functions and feed the coordinates
coordinates(geo1spat) <- ~long+lat
proj4string(geo1spat) <- "+init=epsg:4326"

# Interactive Map visualization with respect to its frequency of occurance
mapview(geo1spat,  burst =FALSE,use.layer.names = FALSE,color = mapviewGetOption("vector.palette"),
        col.regions = mapviewGetOption("vector.palette"), cex = geo1spat$Frequency*0.04,pch=2)










