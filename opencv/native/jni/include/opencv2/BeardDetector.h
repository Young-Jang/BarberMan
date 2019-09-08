#pragma once

#include <opencv2/opencv.hpp>
#include <thread>
#include "MeanShift.h"

class BeardDetector {
private :
#pragma region Deleted_Constructors_And_Operators
	BeardDetector() = delete;
	BeardDetector(const BeardDetector&) = delete;
	BeardDetector(BeardDetector&&) = delete;
	BeardDetector& operator=(const BeardDetector&) = delete;
	BeardDetector& operator=(BeardDetector&&) = delete;
#pragma endregion

private:
/*
	Method to Detect Bounding Box of Input Contour.
	@ param : vector container saved contour interested in.
*/
	auto getBoundary(std::vector<cv::Point>& contour)->std::pair<cv::Point, cv::Point>;

/*
	Method to Set Masks Using Beard Region Detection.
*/
	auto setMasks()->void;

/*
	Method to configure self quotient image by original image.
	@ param : In the method, it use original image what user inputs in initial.
*/
	auto configureSelfQuotient()->void;

/*
	Method to configure mean shift clustered image and label matrix by original image,
	what have flags by inputted hs and hr.
*/
	auto configureMeanShiftCluster()->void;

public  :
/*
	BeardDetector Class Constructor.
	@ param : inputImage is image to analysis that region of beard.
	@		points are feature points about face obtained dlib library.
	@		hs, hr is used to cluster the input image by mean shift algorithm,
	@		they mean radius of distance region and radius of color region at each.
*/
	BeardDetector(cv::Mat inputImage, std::vector<cv::Point> points, int hs = 8, int hr = 5)
		: originalImage(inputImage.clone()), hs(hs), hr(hr), MSClassifier(hs, hr), area(400),
	imageHeight(inputImage.rows), imageWidth(inputImage.cols), channels(inputImage.channels()) {
		if (originalImage.empty()) {
			std::cerr << "Source Image is Invalid : Cannot construct BeardDetector class object (line : 28) \n\n";
			assert(false);
		}
		BeardDetector::points.assign(points.begin(), points.end());
		channels = originalImage.channels();
		region = cv::Mat::zeros(imageHeight, imageWidth, CV_8U);

		configureSelfQuotient();
		configureMeanShiftCluster();
		setMasks();
	}

/*
	Method to Replace Original Image and Process to Detect Beard Region.
	@ param : input Image is to analysis that region of beard.
	@		points are feature points about face obtained dlib library.
*/
	auto replace(cv::Mat inputImage, std::vector<cv::Point> points)->void;

/*
	Method to detect beard region in original image in order to analysis quotient image
	, mean shift clusted image, and label matrix with input threshold to distinguish them.
	@ param : argued already matrices in introduction of this method in class' private member,
			and threshold to distinguish pixel color distance th_color.
*/
	auto detectBeardRegion(int th_color) ->void;

/*
	Method to merge original image, detected region and user marking region in order to show.
	@ param : original image, detected region in private member, and matrix saved marking re-
			gion obtained by user.
*/
	auto drawRegionWithMarking(cv::Mat& markingMat)->cv::Mat;

	#pragma region getMethods
	inline auto getClustered()->cv::Mat {
		return clusteredImage;
	}

	inline auto getQutient()->cv::Mat {
		return quotientImage;
	}

	inline auto getLabel()-> cv::Mat {
		return labelMatrix;
	}

	inline auto getRegion()->cv::Mat {
		return region;
	}
#pragma endregion

private :
	const int hs, hr, area;
	int imageHeight, imageWidth, channels;

	MeanShift MSClassifier;

	cv::Mat originalImage, clusteredImage, quotientImage, labelMatrix, region;
	std::vector<cv::Mat> matrices, q_matrices;
	std::vector<std::pair<cv::Point, cv::Point>> boundaries;
	std::vector<int> skinLabels;
	std::vector<cv::Vec3b> skinColors;
	std::vector<cv::Point> points;
};