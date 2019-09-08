//---------------- Head  File ---------------------------------------
#include "MeanShift.h"
#include <cmath>
#include <thread>

//---------------- Name space ---------------------------------------
using namespace cv;
using namespace std;

//---------------- Definition ---------------------------------------
#define MS_MAX_NUM_CONVERGENCE_STEPS	5										// up to 10 steps are for convergence
#define MS_MEAN_SHIFT_TOL_COLOR			0.09									// minimum mean color shift change
#define MS_MEAN_SHIFT_TOL_SPATIAL		0.09									// minimum mean spatial shift change
const int dxdy[][2] = { {-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1} };	// Region Growing

// Constructor
Point5D::Point5D() {
	x = -1;
	y = -1;
}

// Destructor
Point5D::~Point5D() {
}

// Scale the OpenCV Lab color to Lab range
void Point5D::PointLab() {
	l = l * 100 / 255;
	a = a - 128;
	b = b - 128;
}

// Sclae the Lab color to OpenCV range that can be used to transform to RGB
void Point5D::PointRGB() {
	l = l * 255 / 100;
	a = a + 128;
	b = b + 128;
}

// Accumulate points
void Point5D::MSPoint5DAccum(Point5D Pt) {
	x += Pt.x;
	y += Pt.y;
	l += Pt.l;
	a += Pt.a;
	b += Pt.b;
}

// Copy a point
void Point5D::MSPoint5DCopy(Point5D Pt) {
	x = Pt.x;
	y = Pt.y;
	l = Pt.l;
	a = Pt.a;
	b = Pt.b;
}

// Compute color space distance between two points
float Point5D::MSPoint5DColorDistance(Point5D Pt) {
	return (l - Pt.l) * (l - Pt.l) + (a - Pt.a) * (a - Pt.a) + (b - Pt.b) * (b - Pt.b);
}

// Compute spatial space distance between two points
float Point5D::MSPoint5DSpatialDistance(Point5D Pt) {
	return (x - Pt.x) * (x - Pt.x) + (y - Pt.y) * (y - Pt.y);
}

// Scale point
void Point5D::MSPoint5DScale(float scale) {
	x *= scale;
	y *= scale;
	l *= scale;
	a *= scale;
	b *= scale;
}

// Set point value
void Point5D::MSPOint5DSet(float px, float py, float pl, float pa, float pb) {
	x = px;
	y = py;
	l = pl;
	a = pa;
	b = pb;
}

// Print 5D point
void Point5D::Print() {
	cout << x << " " << y << " " << l << " " << a << " " << b << endl;
}

// Constructor for spatial bandwidth and color bandwidth
MeanShift::MeanShift(float s, float r) {
	hs = s;
	hr = r * r;
}

// Mean Shift Filtering
void MeanShift::MSFiltering(Mat& Img) {
	const auto ROWS = Img.rows;		// Get row number
	const auto COLS = Img.cols;		// Get column number
	split(Img, IMGChannels);		// Split Lab color

	vector<thread> operations;
	const auto div = thread::hardware_concurrency();
	const auto dataSize = ROWS * COLS;
	const auto unit = dataSize / (div - 1);

	for (auto i = 0; i < div; ++i) {
		auto start = unit * i; if (start > dataSize) break;
		auto end = unit * (i + 1); if (end > dataSize) end = dataSize;

		operations.push_back(thread{
			[start, end, &ROWS, &COLS, &Img](int hs, int hr, vector<Mat> IMGChannels)->void {
			int Left, Right, Top, Bottom, NumPts, step;
			Point5D PtCur, PtPrev, PtSum, Pt;
			constexpr auto channels = 3;

			for (auto idx = start; idx < end; ++idx) {
				auto row = idx / COLS, col = idx % COLS;

				Left = (col - hs) > 0 ? (col - hs) : 0;
				Right = (col + hs) < COLS ? (col + hs) : COLS;
				Top = (row - hs) > 0 ? (row - hs) : 0;
				Bottom = (row + hs) < ROWS ? (row + hs) : ROWS;

				PtCur.MSPOint5DSet(row, col, static_cast<float>(IMGChannels[0].data[idx]), static_cast<float>(IMGChannels[1].data[idx]), static_cast<float>(IMGChannels[2].data[idx]));
				PtCur.PointLab();

				step = 0;

				do {
					PtPrev.MSPoint5DCopy(PtCur);
					PtSum.MSPOint5DSet(0, 0, 0, 0, 0);
					NumPts = 0;
					for (auto hx = Top; hx < Bottom; ++hx) {
						for (auto hy = Left; hy < Right; ++hy) {
							auto hIdx = hx * COLS + hy;
							Pt.MSPOint5DSet(hx, hy, static_cast<float>(IMGChannels[0].data[hIdx]), static_cast<float>(IMGChannels[1].data[hIdx]), static_cast<float>(IMGChannels[2].data[hIdx]));
							Pt.PointLab();
							if (Pt.MSPoint5DColorDistance(PtCur) < hr) {
								PtSum.MSPoint5DAccum(Pt);
								NumPts++;
							}
						}
					}
					PtSum.MSPoint5DScale(1.0 / NumPts);
					PtCur.MSPoint5DCopy(PtSum);
					step++;
				} while ((PtCur.MSPoint5DColorDistance(PtPrev) > MS_MEAN_SHIFT_TOL_COLOR) && (PtCur.MSPoint5DSpatialDistance(PtPrev) > MS_MEAN_SHIFT_TOL_SPATIAL) && (step < MS_MAX_NUM_CONVERGENCE_STEPS));
				PtCur.PointRGB();
				auto vIdx = row * COLS * channels + col;
				Img.data[vIdx + 0] = PtCur.l;
				Img.data[vIdx + 1] = PtCur.a;
				Img.data[vIdx + 2] = PtCur.b;
			}
}, hs, hr, IMGChannels
			});
	}

	for (auto& op : operations) op.join();
}

void MeanShift::MSSegmentation(Mat& Img, Mat& Labels) {
	const auto ROWS = Img.rows, COLS = Img.cols;
	split(Img, IMGChannels);

	vector<thread> operations;
	const auto div = thread::hardware_concurrency();
	const auto dataSize = ROWS * COLS;
	const auto unit = dataSize / (div - 1);

	for (auto i = 0; i < div; ++i) {
		auto start = unit * i; if (start > dataSize) break;
		auto end = unit * (i + 1); if (end > dataSize) end = dataSize;

		operations.push_back(thread{
			[start, end, &ROWS, &COLS, &Img](float hs, float hr, vector<Mat> IMGChannels)->void {
			int Left, Right, Top, Bottom, NumPts, step;
			Point5D PtCur, PtPrev, PtSum, Pt;
			constexpr auto channels = 3;

			for (auto idx = start; idx < end; ++idx) {
				auto row = idx / COLS, col = idx % COLS;

				Left = (col - hs) > 0 ? (col - hs) : 0;
				Right = (col + hs) < COLS ? (col + hs) : COLS;
				Top = (row - hs) > 0 ? (row - hs) : 0;
				Bottom = (row + hs) < ROWS ? (row + hs) : ROWS;

				PtCur.MSPOint5DSet(row, col, static_cast<float>(IMGChannels[0].data[idx]), static_cast<float>(IMGChannels[1].data[idx]), static_cast<float>(IMGChannels[2].data[idx]));
				PtCur.PointLab();

				step = 0;

				do {
					PtPrev.MSPoint5DCopy(PtCur);
					PtSum.MSPOint5DSet(0, 0, 0, 0, 0);
					NumPts = 0;
					for (auto hx = Top; hx < Bottom; ++hx) {
						for (auto hy = Left; hy < Right; ++hy) {
							auto hIdx = hx * COLS + hy;
							Pt.MSPOint5DSet(hx, hy, static_cast<float>(IMGChannels[0].data[hIdx]), static_cast<float>(IMGChannels[1].data[hIdx]), static_cast<float>(IMGChannels[2].data[hIdx]));
							Pt.PointLab();
							if (Pt.MSPoint5DColorDistance(PtCur) < hr) {
								PtSum.MSPoint5DAccum(Pt);
								NumPts++;
							}
						}
					}
					PtSum.MSPoint5DScale(1.0 / NumPts);
					PtCur.MSPoint5DCopy(PtSum);
					step++;
				} while ((PtCur.MSPoint5DColorDistance(PtPrev) > MS_MEAN_SHIFT_TOL_COLOR) && (PtCur.MSPoint5DSpatialDistance(PtPrev) > MS_MEAN_SHIFT_TOL_SPATIAL) && (step < MS_MAX_NUM_CONVERGENCE_STEPS));
				PtCur.PointRGB();
				auto vIdx = row * COLS * channels + col * channels;
				Img.data[vIdx + 0] = PtCur.l;
				Img.data[vIdx + 1] = PtCur.a;
				Img.data[vIdx + 2] = PtCur.b;
			}
}, hs, hr, IMGChannels
			});
	}

	for (auto& op : operations) op.join();

	//----------------------- Segmentation ------------------------------
	Point5D PtCur, Pt;
	int label = 0;					// Label number
	float *Mode = new float[ROWS * COLS * 3];					// Store the Lab color of each region
	int *MemberModeCount = new int[ROWS * COLS];				// Store the number of each region
	memset(MemberModeCount, 0, ROWS * COLS * sizeof(int));		// Initialize the MemberModeCount
	split(Img, IMGChannels);

	Labels.create(ROWS, COLS, CV_32S);
	Labels.setTo(0);

	auto ptrLabel = Labels.ptr<int>(0);

	for (int i = 0; i < ROWS; i++) {
		for (int j = 0; j < COLS; j++) {
			auto idx = i * COLS + j;
			// If the point is not being labeled
			if (ptrLabel[idx] == 0) {
				ptrLabel[idx] = ++label;		// Give it a new label number
				// Get the point
				PtCur.MSPOint5DSet(i, j, static_cast<float>(IMGChannels[0].data[idx]), static_cast<float>(IMGChannels[1].data[idx]), static_cast<float>(IMGChannels[2].data[idx]));
				PtCur.PointLab();

				// Store each value of Lab
				Mode[label * 3 + 0] = PtCur.l;
				Mode[label * 3 + 1] = PtCur.a;
				Mode[label * 3 + 2] = PtCur.b;

				// Region Growing 8 Neighbours
				vector<Point5D> NeighbourPoints;
				NeighbourPoints.push_back(PtCur);
				while (!NeighbourPoints.empty()) {
					Pt = NeighbourPoints.back();
					NeighbourPoints.pop_back();

					// Get 8 neighbours
					for (int k = 0; k < 8; k++) {
						int hx = Pt.x + dxdy[k][0];
						int hy = Pt.y + dxdy[k][1];
						auto hIdx = hx * COLS + hy;
						if ((hx >= 0) && (hy >= 0) && (hx < ROWS) && (hy < COLS) && (ptrLabel[hIdx] == 0)) {
							Point5D P;
							P.MSPOint5DSet(hx, hy, static_cast<float>(IMGChannels[0].data[hIdx]), static_cast<float>(IMGChannels[1].data[hIdx]), static_cast<float>(IMGChannels[2].data[hIdx]));
							P.PointLab();

							// Check the color
							if (PtCur.MSPoint5DColorDistance(P) < hr) {
								// Satisfied the color bandwidth
								ptrLabel[hIdx] = label;				// Give the same label					
								NeighbourPoints.push_back(P);		// Push it into stack
								MemberModeCount[label]++;			// This region number plus one
								// Sum all color in same region
								Mode[label * 3 + 0] += P.l;
								Mode[label * 3 + 1] += P.a;
								Mode[label * 3 + 2] += P.b;
							}
						}
					}
				}
				MemberModeCount[label]++;							// Count the point itself
				Mode[label * 3 + 0] /= MemberModeCount[label];		// Get average color
				Mode[label * 3 + 1] /= MemberModeCount[label];
				Mode[label * 3 + 2] /= MemberModeCount[label];
			}
		}
	}

	// Get result image from Mode array
	operations.clear();
	static constexpr auto channels = 3;

	for (auto i = 0; i < div; ++i) {
		auto start = unit * i; if (start > dataSize) break;
		auto end = unit * (i + 1); if (end > dataSize) end = dataSize;

		operations.push_back(thread{
			[start, end, &ptrLabel, &Img, &ROWS, &Mode]()->void {
			int label;
			for (auto idx = start; idx < end; ++idx) {
				auto vIdx = idx * channels;
				label = ptrLabel[idx];
				Img.data[vIdx] = Mode[label * channels] * 255 / 100;
				for (auto added = 1, lIdx = label * channels; added < channels; ++added)
					Img.data[vIdx + added] = Mode[lIdx + added] + 128;
			}
}
			});
	}

	for (auto& op : operations) op.join();

	//--------------- Delete Memory Applied Before -----------------------
	delete[] Mode;
	delete[] MemberModeCount;
}
