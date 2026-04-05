#include <iostream>
#include <vector>
#include <cmath>
#include <algorithm>

using namespace std;

class AI {
private:
    vector<vector<double>> weights;
    double sigmoid(double x) {
        return 1.0 / (1.0 + exp(-x));
    }
    double sigmoid_derivative(double x) {
        return x * (1.0 - x);
    }
    
public:
    AI() {
        weights.resize(10, vector<double>(26, 0.0));
    }
    
    void Shablon_weights() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 26; j++) {
                weights[i][j] = 0.0;
            }
        }
        vector<double> shablonn0 = {
            1,1,1,1,1,
            1,0,0,0,1,
            1,0,0,0,1,
            1,0,0,0,1,
            1,1,1,1,1
        };
        vector<double> shablonn1 = {
            0,0,1,0,0,
            0,1,1,0,0,
            0,0,1,0,0,
            0,0,1,0,0,
            0,1,1,1,0
        };
        vector<double> shablonn2 = {
            1,1,1,1,1,
            0,0,0,0,1,
            1,1,1,1,1,
            1,0,0,0,0,
            1,1,1,1,1
        };
        vector<double> shablonn3 = {
            1,1,1,1,1,
            0,0,0,0,1,
            0,1,1,1,1,
            0,0,0,0,1,
            1,1,1,1,1
        };
        vector<double> shablonn4 = {
            1,0,0,0,1,
            1,0,0,0,1,
            1,1,1,1,1,
            0,0,0,0,1,
            0,0,0,0,1
        };
        vector<double> shablonn5 = {
            1,1,1,1,1,
            1,0,0,0,0,
            1,1,1,1,1,
            0,0,0,0,1,
            1,1,1,1,1
        };
        vector<double> shablonn6 = {
            1,1,1,1,1,
            1,0,0,0,0,
            1,1,1,1,1,
            1,0,0,0,1,
            1,1,1,1,1
        };
        vector<double> shablonn7 = {
            1,1,1,1,1,
            0,0,0,0,1,
            0,0,0,1,0,
            0,0,1,0,0,
            0,1,0,0,0
        };
        vector<double> shablonn8 = {
            1,1,1,1,1,
            1,0,0,0,1,
            1,1,1,1,1,
            1,0,0,0,1,
            1,1,1,1,1
        };
        vector<double> shablonn9 = {
            1,1,1,1,1,
            1,0,0,0,1,
            1,1,1,1,1,
            0,0,0,0,1,
            1,1,1,1,1
        };
        
        vector<vector<double>> shablonns = {shablonn0, shablonn1, shablonn2, shablonn3, shablonn4,shablonn5, shablonn6, shablonn7, shablonn8, shablonn9};

        for (int i = 0; i < 10; i++) {
            for (int ii = 0; ii < 25; ii++) {
                weights[i][ii] = shablonns[i][ii] * 2.0 - 1.0;
            }
            weights[i][25] = 0.5;
        }
    }
    
    int recognize(const vector<vector<int>>& matrix) {
        vector<double> inputs(26, 1.0);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                inputs[i * 5 + j] = matrix[i][j];
            }
        }
        
        vector<double> outputs(10, 0.0);
        int bestShablon = 0;
        double bestScore = -1.0;
        
        for (int digit = 0; digit < 10; digit++) {
            double sum = 0.0;
            for (int i = 0; i < 26; i++) {
                sum += weights[digit][i] * inputs[i];
            }
            outputs[digit] = sigmoid(sum);
            
            if (outputs[digit] > bestScore) {
                bestScore = outputs[digit];
                bestShablon = digit;
            }
        }
        
        return bestShablon;
    }
    
    void train(const vector<vector<int>>& matrix, int targetDigit, double learningRate = 0.1) {
        vector<double> inputs(26, 1.0);
        
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                inputs[i * 5 + j] = matrix[i][j];
            }
        }
        
        for (int digit = 0; digit < 10; digit++) {
            double sum = 0.0;
            for (int i = 0; i < 26; i++) {
                sum += weights[digit][i] * inputs[i];
            }
            double output = sigmoid(sum);
            double target = (digit == targetDigit) ? 1.0 : 0.0;
            double error = target - output;
            
            for (int i = 0; i < 26; i++) {
                weights[digit][i] += learningRate * error * sigmoid_derivative(output) * inputs[i];
            }
        }
    }
};

void print(const vector<vector<int>>& matrix) {
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
            cout << (matrix[i][j] ? "█" : " ");
        }
        cout << endl;
    }
}

int main() {
    AI recognizer;
    recognizer.Shablon_weights();
    vector<vector<int>> matrix(5, vector<int>(5, 0));

    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
            int value;
            cin >> value;
            matrix[i][j] = (value != 0) ? 1 : 0;
        }
    }
    
    print(matrix);
    
    cout << recognizer.recognize(matrix) << endl;
}
