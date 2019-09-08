#include "BeardDetector.h"
using namespace std;
using namespace cv;

template <class T>
ostream& operator<<(ostream& os, vector<T>& vec) {
	for (auto& print : vec) os << print << ' ';
	return os;
}

template <class T1, class T2>
ostream& operator<<(ostream& os, pair<T1, T2>& pair) {
	os << pair.first << ' ' << pair.second;
	return os;
}

inline auto calc(int th)->int {
	return th * th * 3;
}

auto TH_color = calc(45);

inline auto colorDistanceInTH(Vec3b& c1, Vec3b& c2) -> bool {
	Vec3b dist; absdiff(c1, c2, dist);

	return ((dist[0] * dist[0] + dist[1] * dist[1] + dist[2] * dist[2]) < TH_color);
}

// Function to convert source image to grayscale image.
auto cvt2Gray(const Mat& srcImage, Mat& dstImage) -> bool {
	auto channels = srcImage.channels();
	switch (channels) {
	case 4:
		cvtColor(srcImage, dstImage, COLOR_BGRA2GRAY);
		break;
	case 3:
		cvtColor(srcImage, dstImage, COLOR_BGR2GRAY);
		break;
	case 1:
		dstImage = srcImage.clone();
		break;
	default:
		cerr << "Source image has invalid channels to convert to mono-scale image.\n";
		return false;
	}
	return true;
}

// Function to convert source image to Lab color scale image.
auto cvt2Lab(const Mat& srcImage, Mat& dstImage) -> bool {
	auto channels = srcImage.channels();
	dstImage = srcImage.clone();

	switch (channels) {
	case 4:
		cvtColor(dstImage, dstImage, COLOR_BGRA2BGR);
	case 3:
		cvtColor(dstImage, dstImage, COLOR_BGR2Lab);
		break;
	default:
		cerr << "Source image has invalid channels to convert to mono-scale image.\n";
		return false;
	}
	return true;
}

auto BeardDetector::getBoundary(vector<Point>& contour)->pair<Point, Point> {
	auto minCol = imageWidth, minRow = imageHeight, maxCol = -1, maxRow = -1;

	for (auto& pt : contour) {
		if (minCol > pt.x) minCol = pt.x;
		if (maxCol < pt.x) maxCol = pt.x;
		if (minRow > pt.y) minRow = pt.y;
		if (maxRow < pt.y) maxRow = pt.y;
	}

	return make_pair(Point{ minCol, minRow }, Point{ maxCol, maxRow });
}

auto BeardDetector::replace(cv::Mat inputImage, std::vector<cv::Point> points)->void {
	originalImage = inputImage.clone();
	BeardDetector::points.assign(points.begin(), points.end());
	imageHeight = originalImage.rows, imageWidth = originalImage.cols;
	channels = originalImage.channels();

	matrices.clear(); q_matrices.clear();
	boundaries.clear(); skinLabels.clear(); skinColors.clear();

	region.create(imageHeight, imageWidth, CV_8U);
	region.setTo(0);

	configureSelfQuotient();
	configureMeanShiftCluster();
	setMasks();
}

auto BeardDetector::setMasks()->void {
	vector<vector<Point>> contours(5);

	contours[0].push_back((points[47] * 5 + points[30]) / 6);
	contours[0].push_back(points[27]);
	contours[0].push_back((points[40] * 5 + points[30]) / 6);
	contours[0].push_back(points[30]);

	contours[1].assign(points.begin() + 2, points.begin() + 7);
	contours[1].push_back(points[48]);
	contours[1].push_back((points[2] + points[31] * 3) / 4);

	contours[2].assign(points.begin() + 10, points.begin() + 15);
	contours[2].push_back((points[14] + points[35] * 3) / 4);
	contours[2].push_back(points[54]);

	contours[3].assign(points.begin() + 48, points.begin() + 55);
	for (auto it = points.begin() + 35; it != points.begin() + 30; --it)
		contours[3].push_back(*it);

	contours[4].assign(points.begin() + 6, points.begin() + 11);
	contours[4].insert(contours[4].end(), points.begin() + 54, points.begin() + 60);
	contours[4].push_back(points[48]);

	Mat emptyMat(imageHeight, imageWidth, CV_8U);

	for (auto i = 0; i < contours.size(); ++i) {
		emptyMat.setTo(0);
		drawContours(emptyMat, contours, i, cv::Scalar::all(255), -1);
		matrices.push_back(emptyMat.clone());
	}

	auto ptrLabel = labelMatrix.ptr<int>(0);

	auto boundary = getBoundary(contours[0]);
	for (auto row = boundary.first.y; row < boundary.second.y; ++row) {
		for (auto col = boundary.first.x; col < boundary.second.x; ++col) {
			auto idx = row * imageWidth + col;
			auto val = ptrLabel[idx];
			if (matrices[0].data[idx]) {
				auto k = 0;
				for (; k < skinLabels.size(); ++k)
					if (skinLabels[k] == val) break;
				if (k == skinLabels.size()) {
					skinLabels.push_back(val);
					idx *= channels;
					skinColors.emplace_back(clusteredImage.data[idx + 0],
						clusteredImage.data[idx + 1], clusteredImage.data[idx + 2]);
				}
			}
		}
	}

	for (auto i = 1; i < contours.size(); ++i)
		boundaries.emplace_back(getBoundary(contours[i]));

	Mat temp;
	for (auto& mask : matrices) {
		quotientImage.copyTo(temp, mask);
		q_matrices.push_back(temp.clone());
		temp.setTo(0);
	}
}

auto BeardDetector::configureSelfQuotient()->void {
	constexpr auto maxVal = 255, thresholdVal = 175;
	static constexpr auto kernelSize = 5, paddingSize = kernelSize / 2;

	Mat grayImage;
	assert(cvt2Gray(originalImage, grayImage));

	auto gaussianKernel = getGaussianKernel(kernelSize, -1, CV_32F);
	gaussianKernel *= gaussianKernel.t();
	auto ptrGaussian = gaussianKernel.ptr<float>(0);

	Mat paddingMatrix;
	copyMakeBorder(grayImage, paddingMatrix, paddingSize, paddingSize,
		paddingSize, paddingSize, BORDER_REFLECT101);

	Mat dstImage(imageHeight, imageWidth, CV_8U);


	vector<thread> operations;
	const int div = thread::hardware_concurrency();
	const auto unit = imageHeight / (div - 1);

	for (auto i = 0; i < div; ++i) {
		auto start = unit * i; if (start > imageHeight) break;
		auto end = unit * (i + 1); if (end > imageHeight) end = imageHeight;

		operations.push_back(thread{
			[start, end, &dstImage, &paddingMatrix, &grayImage, &ptrGaussian](const int imageWidth)->void {
			auto windowSize = kernelSize * kernelSize;
			int mean; float weight, accumulation;
			Mat window;

			for (auto row = start; row < end; ++row) {
				for (auto col = 0; col < imageWidth; ++col) {
					window = paddingMatrix(Range{ row, row + kernelSize }, Range{ col, col + kernelSize }).clone();
					mean = 0; weight = accumulation = 0.f;
					for (auto index = 0; index < windowSize; ++index)
						mean += static_cast<int>(window.data[index]);
					mean /= windowSize;

					for (auto index = 0; index < windowSize; ++index) {
						if (window.data[index] >= mean) {
							weight += ptrGaussian[index];
							accumulation += ptrGaussian[index] * window.data[index];
						}
					}

					weight *= windowSize;
					weight = 1.f / weight;

					dstImage.data[row * imageWidth + col] = static_cast<uchar>(
						static_cast<float>(grayImage.data[row * imageWidth + col])
						/ (weight * accumulation));
				}
			}
		}, imageWidth
			});
	}

	for (auto& op : operations) op.join();
	equalizeHist(dstImage, dstImage);

	dstImage = maxVal - dstImage;
	threshold(dstImage, dstImage, thresholdVal, NULL, THRESH_TOZERO);
	equalizeHist(dstImage, quotientImage);
}

auto BeardDetector::configureMeanShiftCluster()->void {
	assert(cvt2Lab(originalImage, clusteredImage));
	MSClassifier.MSSegmentation(clusteredImage, labelMatrix);
	cvtColor(clusteredImage, clusteredImage, COLOR_Lab2BGR);
}

auto BeardDetector::detectBeardRegion(int th_color) ->void {
	TH_color = calc(th_color);

	region.setTo(0);

	auto ptrLabel = labelMatrix.ptr<int>(0);
	for (auto i = 0; i < boundaries.size(); ++i) {
		auto minCol = boundaries[i].first.x, minRow = boundaries[i].first.y;
		auto maxCol = boundaries[i].second.x, maxRow = boundaries[i].second.y;

		vector<int> labelVec, detected;

		for (auto row = minRow; row <= maxRow; ++row) {
			for (auto col = minCol; col <= maxCol; ++col) {
				auto idx = row * imageWidth + col;
				if (q_matrices[i + 1].data[idx]) {
					auto label = ptrLabel[idx];
					auto lIdx = 0;
					for (; lIdx < labelVec.size(); ++lIdx)
						if (labelVec[lIdx] == label) break;
					if (lIdx == labelVec.size()) {
						labelVec.push_back(label);
						Vec3b color{ clusteredImage.data[idx * channels],
							clusteredImage.data[idx * channels + 1],
							clusteredImage.data[idx * channels + 2] };
						for (lIdx = 0; lIdx < skinLabels.size(); ++lIdx)
							if (skinLabels[lIdx] == label
								|| colorDistanceInTH(skinColors[lIdx], color))
								break;
						if (lIdx == skinLabels.size())
							detected.push_back(label);
					}
				}
			}
		}

		for (auto row = minRow; row <= maxRow; ++row) {
			for (auto col = minCol; col <= maxCol; ++col) {
				auto idx = row * imageWidth + col;

				if (matrices[i + 1].data[idx]) {
					auto flag = false;
					auto label = ptrLabel[idx];

					for (auto& cmp : detected)
						if (cmp == label) {
							flag = true; break;
						}

					if (flag)
						region.data[idx] = 1;
				}
			}
		}
	}

	vector<vector<cv::Point>> contours;
	findContours(region, contours, RETR_EXTERNAL, CHAIN_APPROX_NONE);
	for (auto it = contours.begin(); it != contours.end();) {
		if (contourArea(*it) < area) {
			it = contours.erase(it);
		}
		else {
			cout << contourArea(*it) << '\n';
			++it;
		}
	}
	drawContours(region, contours, -1, Scalar::all(255), -1);
}

auto BeardDetector::drawRegionWithMarking(Mat& markingMat)->Mat {
	const auto div = thread::hardware_concurrency();
	const auto dataSize = imageHeight * imageWidth;
	const auto unit = dataSize / (div - 1);

	auto retImage = originalImage.clone();
	auto regionImage = region.clone();

	vector<thread> operations;

	for (auto i = 0; i < div; ++i) {
		auto start = unit * i; if (start > dataSize) break;
		auto end = unit * (i + 1); if (end > dataSize) end = dataSize;

		operations.push_back(thread{
			[start, end, &retImage, &regionImage, &markingMat](int channels)->void {
			for (auto idx = start; idx < end; ++idx) {
				if (markingMat.empty()) {
					if (regionImage.data[idx])
						retImage.data[idx * channels + 1] += 50;
				}
				else if (markingMat.data[idx] == 255 ||
					(regionImage.data[idx] && markingMat.data[idx] != 127))
					retImage.data[idx * channels + 1] += 50;
			}
		}, channels
			});
	}

	for (auto& op : operations) op.join();

	return retImage;
}